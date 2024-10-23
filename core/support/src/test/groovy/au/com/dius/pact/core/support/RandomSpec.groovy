package au.com.dius.pact.core.support

import spock.lang.Issue
import spock.lang.Specification

class RandomSpec extends Specification {
  def 'generates a random value from the regular expression'() {
    expect:
    Random.generateRandomString('\\w+') ==~ /\w+/
  }

  @Issue('#1826')
  def 'handles regex anchors'() {
    expect:
    Random.generateRandomString('^\\w+$') ==~ /\w+/
  }

  def 'does not remove escaped values'() {
    expect:
    Random.generateRandomString('\\^\\w+\\$') ==~ /\^\w+\$/
  }
}
