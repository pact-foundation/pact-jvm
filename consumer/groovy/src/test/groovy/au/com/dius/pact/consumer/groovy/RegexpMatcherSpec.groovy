package au.com.dius.pact.consumer.groovy

import spock.lang.Issue
import spock.lang.Specification

class RegexpMatcherSpec extends Specification {
  def 'returns the value provided to the constructor'() {
    expect:
    new RegexpMatcher('\\w+', 'word').value == 'word'
  }

  def 'if no value is provided to the constructor, generates a random value when needed'() {
    expect:
    new RegexpMatcher('\\w+', null).value ==~ /\w+/
  }

  @Issue('#1826')
  def 'handles regex anchors'() {
    expect:
    new RegexpMatcher('^\\w+$', null).value ==~ /\w+/
  }
}
