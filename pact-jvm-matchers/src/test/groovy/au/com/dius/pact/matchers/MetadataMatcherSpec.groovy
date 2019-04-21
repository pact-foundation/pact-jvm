package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.model.matchingrules.RegexMatcher
import spock.lang.Specification

class MetadataMatcherSpec extends Specification {

  def "be true when metadatas are equal"() {
    expect:
    MetadataMatcher.INSTANCE.compare('X', 'A', 'A', new MatchingRulesImpl()) == null
    MetadataMatcher.INSTANCE.compare('X', null, null, new MatchingRulesImpl()) == null
  }

  def "be false when metadatas are not equal"() {
    expect:
    MetadataMatcher.INSTANCE.compare('X', 'A', 'B', new MatchingRulesImpl()) != null
    MetadataMatcher.INSTANCE.compare('X', 'A', null, new MatchingRulesImpl()) != null
  }

  def "supports collections"() {
    expect:
    MetadataMatcher.INSTANCE.compare('X', ['A'], ['A'], new MatchingRulesImpl()) == null
    MetadataMatcher.INSTANCE.compare('X', ['A'], ['A', 'B'], new MatchingRulesImpl()) != null
  }

  def "delegate to a matcher when one is defined"() {
    given:
    def matchers = new MatchingRulesImpl()
    matchers.addCategory('metaData').addRule('S', new RegexMatcher('\\w\\d+'))

    expect:
    MetadataMatcher.INSTANCE.compare('S', 'X001', 'Z155411', matchers) == null
  }

  def "combines mismatches if there are multiple"() {
    given:
    def matchers = new MatchingRulesImpl()
    def category = matchers.addCategory('metaData')
    category.addRule('X', new RegexMatcher('X=.*'))
    category.addRule('X', new RegexMatcher('A=.*'))
    category.addRule('X', new RegexMatcher('B=\\w\\d+'))

    expect:
    MetadataMatcher.INSTANCE.compare('X', 'HEADER', 'X=YZ', matchers).mismatch ==
      "Expected 'X=YZ' to match 'A=.*', Expected 'X=YZ' to match 'B=\\w\\d+'"
  }
}
