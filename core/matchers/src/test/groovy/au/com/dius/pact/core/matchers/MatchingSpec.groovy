package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactReaderKt
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings(['LineLength', 'PrivateFieldCouldBeFinal'])
class MatchingSpec extends Specification {

  private static Request request

  private static RequestResponseInteraction interaction = new RequestResponseInteraction()

  def setup() {
    request = new Request('GET', '/', PactReaderKt.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test": true}'))
  }

  def 'Body Matching - Handle both None'() {
    expect:
    Matching.INSTANCE.matchBody(new Request('', '', null, ['Content-Type': 'a']),
      new Request('', '', null, ['Content-Type': 'a']), true).empty
  }

  def 'Body Matching - Handle left None'() {
    expect:
    Matching.INSTANCE.matchBody(new Request('', '', null, ['Content-Type': 'a'], request.body),
      new Request('', '', null, ['Content-Type': 'a']), true).contains(mismatch)

    where:
    mismatch = new BodyMismatch(request.body.value, null, 'Expected body \'{"test": true}\' but was missing')
  }

  def 'Body Matching - Handle right None'() {
    expect:
    Matching.INSTANCE.matchBody(new Request('', '', null, ['Content-Type': 'a']),
      new Request('', '', null, ['Content-Type': 'a'], request.body), true).empty
  }

  def 'Body Matching - Handle different mime types'() {
    expect:
    Matching.INSTANCE.matchBody(new Request('', '', null, ['Content-Type': 'a'], request.body),
      new Request('', '', null, ['Content-Type': 'b'], request.body), true) == mismatch

    where:
    mismatch = [ new BodyTypeMismatch('a', 'b') ]
  }

  def 'Body Matching - match different mimetypes by regexp'() {
    expect:
    Matching.INSTANCE.matchBody(new Request('', '', null, ['Content-Type': 'application/x+json'], body),
      new Request('', '', null, ['Content-Type': 'application/x+json'], body), true).empty

    where:
    body = OptionalBody.body('{ "name":  "bob" }')
  }

  @Unroll
  def 'Method matching - #desc'() {
    expect:
    Matching.INSTANCE.matchMethod(valA, valB) == result

    where:

    desc                 | valA | valB | result
    'match same'         | 'a'  | 'a'  | null
    'match ignore case'  | 'a'  | 'A'  | null
    'mismatch different' | 'a'  | 'b'  | new MethodMismatch('a', 'b')
  }

  private query(String queryString = '') {
    new Request('', '', PactReaderKt.queryStringToMap(queryString), null, OptionalBody.body(''), null)
  }

  def 'Query Matching - match same'() {
    expect:
    Matching.INSTANCE.matchQuery(query('a=b'), query('a=b')).empty
  }

  def 'Query Matching - match none'() {
    expect:
    Matching.INSTANCE.matchQuery(query(), query()).empty
  }

  def 'Query Matching - mismatch none to something'() {
    expect:
    Matching.INSTANCE.matchQuery(query(), query('a=b')) == mismatch

    where:
    mismatch = [ new QueryMismatch('a', '', 'b',
      "Unexpected query parameter 'a' received", '$.query.a') ]
  }

  def 'Query Matching - mismatch something to none'() {
    expect:
    Matching.INSTANCE.matchQuery(query('a=b'), query()) == mismatch

    where:
    mismatch = [ new QueryMismatch('a', 'b', '',
      "Expected query parameter 'a' but was missing", '$.query.a') ]
  }

  def 'Query Matching - match keys in different order'() {
    expect:
    Matching.INSTANCE.matchQuery(query('status=RESPONSE_RECEIVED&insurerCode=ABC'),
      query('insurerCode=ABC&status=RESPONSE_RECEIVED')).empty
  }

  def 'Query Matching - mismatch if the same key is repeated with values in different order'() {
    expect:
    Matching.INSTANCE.matchQuery(query('a=1&a=2&b=3'), query('a=2&a=1&b=3')) == mismatch

    where:
    mismatch = [
      new QueryMismatch('a', '1', '2',
        "Expected '1' but received '2' for query parameter 'a'", 'a'),
      new QueryMismatch('a', '2', '1',
        "Expected '2' but received '1' for query parameter 'a'", 'a')
    ]
  }

  def 'Header Matching - match empty'() {
    expect:
    Matching.INSTANCE.matchHeaders(new Request('', '', null), new Request('', '', null)).empty
  }

  def 'Header Matching - match same headers'() {
    expect:
    Matching.INSTANCE.matchHeaders(new Request('', '', null, [A: 'B']),
      new Request('', '', null, [A: 'B'])).empty
  }

  def 'Header Matching - ignore additional headers'() {
    expect:
    Matching.INSTANCE.matchHeaders(new Request('', '', null, [A: 'B']),
      new Request('', '', null, [A: 'B', C: 'D'])).empty
  }

  def 'Header Matching - complain about missing headers'() {
    expect:
    Matching.INSTANCE.matchHeaders(new Request('', '', null, [A: 'B', C: 'D']),
      new Request('', '', null, [A: 'B'])) == mismatch

    where:
    mismatch = [
      new HeaderMismatch('C', 'D', '', "Expected a header 'C' but was missing")
    ]
  }

  def 'Header Matching - complain about incorrect headers'() {
    expect:
    Matching.INSTANCE.matchHeaders(new Request('', '', null, [A: 'B']),
      new Request('', '', null, [A: 'C'])) == mismatch

    where:
    mismatch = [
      new HeaderMismatch('A', 'B', 'C', "Expected header 'A' to have value 'B' but was 'C'")
    ]
  }

  @Unroll
  def 'merging request matchers'() {
    expect:
    a.merge(b) == result

    where:

    a                                            | b                                            | result
    new FullRequestMatch(interaction)            | new FullRequestMatch(interaction)            | new FullRequestMatch(interaction)
    new FullRequestMatch(interaction)            | new PartialRequestMatch([(interaction): []]) | new FullRequestMatch(interaction)
    new PartialRequestMatch([(interaction): []]) | new FullRequestMatch(interaction)            | new FullRequestMatch(interaction)
    new PartialRequestMatch([(interaction): []]) | new PartialRequestMatch([(interaction): []]) | new PartialRequestMatch([(interaction): []])
    RequestMismatch.INSTANCE                     | new FullRequestMatch(interaction)            | new FullRequestMatch(interaction)
    new FullRequestMatch(interaction)            | RequestMismatch.INSTANCE                     | new FullRequestMatch(interaction)
    RequestMismatch.INSTANCE                     | new PartialRequestMatch([(interaction): []]) | new PartialRequestMatch([(interaction): []])
    new PartialRequestMatch([(interaction): []]) | RequestMismatch.INSTANCE                     | new PartialRequestMatch([(interaction): []])
    RequestMismatch.INSTANCE                     | RequestMismatch.INSTANCE                     | RequestMismatch.INSTANCE
  }
}
