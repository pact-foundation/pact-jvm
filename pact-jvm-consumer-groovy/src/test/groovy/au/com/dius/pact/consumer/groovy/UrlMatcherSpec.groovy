package au.com.dius.pact.consumer.groovy

import spock.lang.Specification

class UrlMatcherSpec extends Specification {

  def 'converts groovy regex matcher class to matching rule regex class'() {
    when:
    def matcher = new UrlMatcher('http://localhost:8080',
      ['a', new RegexpMatcher(value: '123', regex: '\\d+'), 'b'])

    then:
    matcher.matcher.toMap() == [match: 'regex', regex: '.*\\Qa\\E\\/\\d+\\/\\Qb\\E$' ]
  }
}
