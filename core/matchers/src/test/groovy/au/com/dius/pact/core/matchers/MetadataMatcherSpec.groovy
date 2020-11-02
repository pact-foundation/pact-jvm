package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import spock.lang.Specification

class MetadataMatcherSpec extends Specification {

  private MatchingContext context

  def setup() {
    context = new MatchingContext(new MatchingRuleCategory('metadata'), true)
  }

  def "be true when metadatas are equal"() {
    expect:
    MetadataMatcher.INSTANCE.compare('X', 'A', 'A', context) == null
    MetadataMatcher.INSTANCE.compare('X', null, null, context) == null
  }

  def "be false when metadatas are not equal"() {
    expect:
    MetadataMatcher.INSTANCE.compare('X', 'A', 'B', context) != null
    MetadataMatcher.INSTANCE.compare('X', 'A', null, context) != null
  }

  def "supports collections"() {
    expect:
    MetadataMatcher.INSTANCE.compare('X', ['A'], ['A'], context) == null
    MetadataMatcher.INSTANCE.compare('X', ['A'], ['A', 'B'], context) != null
  }

  def "delegate to a matcher when one is defined"() {
    given:
    context.matchers.addRule('S', new RegexMatcher('\\w\\d+'))

    expect:
    MetadataMatcher.INSTANCE.compare('S', 'X001', 'Z155411', context) == null
  }

  def "combines mismatches if there are multiple"() {
    given:
    context.matchers.addRule('X', new RegexMatcher('X=.*'))
    context.matchers.addRule('X', new RegexMatcher('A=.*'))
    context.matchers.addRule('X', new RegexMatcher('B=\\w\\d+'))

    expect:
    MetadataMatcher.INSTANCE.compare('X', 'HEADER', 'X=YZ', context).mismatch ==
      "Expected 'X=YZ' to match 'A=.*', Expected 'X=YZ' to match 'B=\\w\\d+'"
  }
}
