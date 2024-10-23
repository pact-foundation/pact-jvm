package au.com.dius.pact.core.model.generators

import spock.lang.Issue
import spock.lang.Specification

class RegexGeneratorSpec extends Specification {
  def 'generates a random value when needed'() {
    expect:
    new RegexGenerator('\\w+').generate([:], '') ==~ /\w+/
  }

  @Issue('#1826')
  def 'handles regex anchors'() {
    expect:
    new RegexGenerator('^\\w+$').generate([:], '') ==~ /\w+/
  }
}
