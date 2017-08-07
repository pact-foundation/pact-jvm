package au.com.dius.pact.model

import scala.None$
import scala.Some
import scala.collection.JavaConversions
import spock.lang.Specification
import spock.lang.Unroll

class MatchingSpec extends Specification {

  private static Request request

  def setup() {
    /*

    val provider = new Provider("test_provider")
    val consumer = new Consumer("test_consumer")
    */

    request = new Request('GET', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test": true}'))

    /*
    val response = new Response(200, JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval")),
      OptionalBody.body("{\"responsetest\": true}"))

    val interaction = new RequestResponseInteraction("test interaction", Seq(new ProviderState("test state")).asJava,
      request, response)

    val interactions = util.Arrays.asList(interaction)

    val pact: RequestResponsePact = new RequestResponsePact(provider, consumer, interactions)

    */
  }

  def 'Body Matching - Handle both None'() {
    expect:
    Matching.matchBody(new Request('', '', null, ['Content-Type': 'a']),
      new Request('', '', null, ['Content-Type': 'a']), true).isEmpty()
  }

  def 'Body Matching - Handle left None'() {
    expect:
    Matching.matchBody(new Request('', '', null, ['Content-Type': 'a'], request.body),
      new Request('', '', null, ['Content-Type': 'a']), true) == mismatch

    where:
    mismatch = JavaConversions.asScalaBuffer([ BodyMismatch.apply(request.body.value, None$.MODULE$, Some.apply(
      'Expected body \'{"test": true}\' but was missing'), '/', None$.MODULE$) ]).toSeq()
  }

  def 'Body Matching - Handle right None'() {
    expect:
    Matching.matchBody(new Request('', '', null, ['Content-Type': 'a']),
      new Request('', '', null, ['Content-Type': 'a'], request.body), true).isEmpty()
  }

  def 'Body Matching - Handle different mime types'() {
    expect:
    Matching.matchBody(new Request('', '', null, ['Content-Type': 'a'], request.body),
      new Request('', '', null, ['Content-Type': 'b'], request.body), true) == mismatch

    where:
    mismatch = JavaConversions.asScalaBuffer([ BodyTypeMismatch.apply('a', 'b') ]).toSeq()
  }

  def 'Body Matching - match different mimetypes by regexp'() {
    expect:
    Matching.matchBody(new Request('', '', null, ['Content-Type': 'application/x+json'], body),
      new Request('', '', null, ['Content-Type': 'application/x+json'], body), true).isEmpty()

    where:
    body = OptionalBody.body('{ "name":  "bob" }')
  }

  @Unroll
  def 'Method matching - #desc'() {
    expect:
    Matching.matchMethod(valA, valB) == result

    where:

    desc                 | valA | valB | result
    'match same'         | 'a'  | 'a'  | None$.MODULE$
    'match ignore case'  | 'a'  | 'A'  | None$.MODULE$
    'mismatch different' | 'a'  | 'b'  | Some.apply(MethodMismatch.apply('a', 'b'))
  }

  private query(String queryString = '') {
    new Request('', '', PactReader.queryStringToMap(queryString), null, OptionalBody.body(''), null)
  }

  def 'Query Matching - match same'() {
    expect:
    Matching.matchQuery(query('a=b'), query('a=b')).empty
  }

  def 'Query Matching - match none'() {
    expect:
    Matching.matchQuery(query(), query()).empty
  }

  def 'Query Matching - mismatch none to something'() {
    expect:
    Matching.matchQuery(query(), query('a=b')) == mismatch

    where:
    mismatch = JavaConversions.asScalaBuffer([ QueryMismatch.apply('a', '', 'b',
      Some.apply("Unexpected query parameter 'a' received"), '$.query.a') ]).toSeq()
  }

  def 'Query Matching - mismatch something to none'() {
    expect:
    Matching.matchQuery(query('a=b'), query()) == mismatch

    where:
    mismatch = JavaConversions.asScalaBuffer([ QueryMismatch.apply('a', 'b', '',
      Some.apply("Expected query parameter 'a' but was missing"), '$.query.a') ]).toSeq()
  }

  def 'Query Matching - match keys in different order'() {
    expect:
    Matching.matchQuery(query('status=RESPONSE_RECEIVED&insurerCode=ABC'),
      query('insurerCode=ABC&status=RESPONSE_RECEIVED')).empty
  }

  def 'Query Matching - mismatch if the same key is repeated with values in different order'() {
    expect:
    Matching.matchQuery(query('a=1&a=2&b=3'), query('a=2&a=1&b=3')) == mismatch

    where:
    mismatch = JavaConversions.asScalaBuffer([
      QueryMismatch.apply('a', '1', '2',
        Some.apply("Expected '1' but received '2' for query parameter 'a'"), 'a'),
      QueryMismatch.apply('a', '2', '1',
        Some.apply("Expected '2' but received '1' for query parameter 'a'"), 'a')
    ]).toSeq()
  }

  def 'Header Matching - match empty'() {
    expect:
    Matching.matchHeaders(new Request('', '', null), new Request('', '', null)).empty
  }

  def 'Header Matching - match same headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', null, [A: 'B']),
      new Request('', '', null, [A: 'B'])).empty
  }

  def 'Header Matching - ignore additional headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', null, [A: 'B']),
      new Request('', '', null, [A: 'B', C: 'D'])).empty
  }

  def 'Header Matching - complain about missing headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', null, [A: 'B', C: 'D']),
      new Request('', '', null, [A: 'B'])) == mismatch

    where:
    mismatch = JavaConversions.asScalaBuffer([
      HeaderMismatch.apply('C', 'D', '', Some.apply("Expected a header 'C' but was missing"))
    ]).toSeq()
  }

  def 'Header Matching - complain about incorrect headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', null, [A: 'B']),
      new Request('', '', null, [A: 'C'])) == mismatch

    where:
    mismatch = JavaConversions.asScalaBuffer([
      HeaderMismatch.apply('A', 'B', 'C', Some.apply("Expected header 'A' to have value 'B' but was 'C'"))
    ]).toSeq()
  }

}
