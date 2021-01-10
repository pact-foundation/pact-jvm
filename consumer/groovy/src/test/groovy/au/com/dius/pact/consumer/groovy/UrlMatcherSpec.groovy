package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.PactSpecVersion
import spock.lang.Specification

class UrlMatcherSpec extends Specification {

  def 'converts groovy regex matcher class to matching rule regex class'() {
    when:
    def matcher = new UrlMatcher('http://localhost:8080',
      ['a', new RegexpMatcher('\\d+', '123'), 'b'])

    then:
    matcher.matcher.toMap(PactSpecVersion.V3) == [match: 'regex', regex: '.*\\/(\\Qa\\E\\/\\d+\\/\\Qb\\E)$' ]
  }
}
