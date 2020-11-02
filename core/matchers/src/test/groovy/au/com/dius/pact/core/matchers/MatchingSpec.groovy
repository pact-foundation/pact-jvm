package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactReaderKt
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.matchingrules.ContentTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import spock.lang.Specification

class MatchingSpec extends Specification {

  private static Request request
  private MatchingContext headerContext
  private MatchingContext metadataContext
  private MatchingContext bodyContext

  def setup() {
    request = new Request('GET', '/', PactReaderKt.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test": true}'.bytes))
    headerContext = new MatchingContext(new MatchingRuleCategory('header'), true)
    metadataContext = new MatchingContext(new MatchingRuleCategory('header'), true)
    bodyContext = new MatchingContext(new MatchingRuleCategory('body'), true)
  }

  def 'Header Matching - match empty'() {
    expect:
    Matching.matchHeaders(new Request('', ''), new Request('', ''), headerContext).empty
  }

  def 'Header Matching - match same headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', [:], [A: ['B']]),
      new Request('', '', [:], [A: ['B']]), headerContext) == [new HeaderMatchResult('A', [])]
  }

  def 'Header Matching - ignore additional headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', [:], [A: ['B']]),
      new Request('', '', [:], [A: ['B'], C: ['D']]), headerContext) == [new HeaderMatchResult('A', [])]
  }

  def 'Header Matching - complain about missing headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', [:], [A: ['B'], C: ['D']]),
      new Request('', '', [:], [A: ['B']]), headerContext) == [
        new HeaderMatchResult('A', []),
        new HeaderMatchResult('C', [mismatch])
      ]

    where:
    mismatch = new HeaderMismatch('C', 'D', '', "Expected a header 'C' but was missing")
  }

  def 'Header Matching - complain about incorrect headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', [:], [A: ['B']]),
      new Request('', '', [:], [A: ['C']]), headerContext) == [new HeaderMatchResult('A', [mismatch])]

    where:
    mismatch = new HeaderMismatch('A', 'B', 'C', "Expected header 'A' to have value 'B' but was 'C'")
  }

  def 'Metadata Matching - match empty'() {
    expect:
    Matching.compareMessageMetadata([:], [:], metadataContext).empty
  }

  def 'Metadata Matching - match same metadata'() {
    expect:
    Matching.compareMessageMetadata([x: 1], [x: 1], metadataContext).empty
  }

  def 'Metadata Matching - ignore additional keys'() {
    expect:
    Matching.compareMessageMetadata([A: 'B'], [A: 'B', C: 'D'], metadataContext).empty
  }

  def 'Metadata Matching - complain about missing keys'() {
    expect:
    Matching.compareMessageMetadata([A: 'B', C: 'D'], [A: 'B'], metadataContext) == mismatch

    where:
    mismatch = [
      new MetadataMismatch('C', 'D', null, "Expected metadata 'C' but was missing")
    ]
  }

  def 'Metadata Matching - complain about incorrect keys'() {
    expect:
    Matching.compareMessageMetadata([A: 'B'], [A: 'C'], metadataContext) == mismatch

    where:
    mismatch = [
      new MetadataMismatch('A', 'B', 'C',
        "Expected metadata key 'A' to have value 'B' (String) but was 'C' (String)")
    ]
  }

  def 'Metadata Matching - ignores missing content type'() {
    expect:
    Matching.compareMessageMetadata([A: 'B', contentType: 'D'], [A: 'B'], metadataContext).empty
  }

  def 'Body Matching - compares the bytes of the body'() {
    expect:
    Matching.INSTANCE.matchBody(expected, actual, bodyContext).mismatches.empty

    where:

    expected = new Response(200, [:], OptionalBody.body([1, 2, 3, 4] as byte[]))
    actual = new Response(200, [:], OptionalBody.body([1, 2, 3, 4] as byte[]))
  }

  def 'Body Matching - compares the bytes of the body with text'() {
    expect:
    Matching.INSTANCE.matchBody(expected, actual, bodyContext).mismatches.empty

    where:

    expected = new Response(200, [:], OptionalBody.body('hello'.bytes))
    actual = new Response(200, [:], OptionalBody.body('hello'.bytes))
  }

  def 'Body Matching - compares the body with any defined matcher'() {
    given:
    def matchingRulesImpl = new MatchingRulesImpl()
    matchingRulesImpl.addCategory('body').addRule('$', new ContentTypeMatcher('image/jpeg'))
    def expected = new Response(200, ['Content-Type': ['image/jpeg']], OptionalBody.body('hello'.bytes),
      matchingRulesImpl)
    def actual = new Response(200, [:], OptionalBody.body(MatchingSpec.getResourceAsStream('/RAT.JPG').bytes))

    when:
    def result = Matching.INSTANCE.matchBody(expected, actual, bodyContext)

    then:
    result.mismatches.empty
  }

  def 'Body Matching - only use a matcher that can handle the body type'() {
    given:
    def matchingRulesImpl = new MatchingRulesImpl()
    matchingRulesImpl.addCategory('body').addRule('$', TypeMatcher.INSTANCE)
    def expected = new Response(200, ['Content-Type': ['image/jpeg']], OptionalBody.body('hello'.bytes),
      matchingRulesImpl)
    def actual = new Response(200, [:], OptionalBody.body(MatchingSpec.getResourceAsStream('/RAT.JPG').bytes))

    when:
    def result = Matching.INSTANCE.matchBody(expected, actual, bodyContext).mismatches

    then:
    !result.empty
    result[0].mismatch.endsWith("is not equal to the expected body 'hello'")
  }
}
