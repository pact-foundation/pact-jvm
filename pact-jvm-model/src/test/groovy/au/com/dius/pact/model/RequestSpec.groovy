package au.com.dius.pact.model

import spock.lang.Specification

class RequestSpec extends Specification {

  def 'delegates to the matching rules to parse matchers'() {
    given:
    def json = [
      matchingRules: [
        'stuff': [:]
      ]
    ]

    when:
    def request = Request.fromMap(json)

    then:
    !request.matchingRules.empty
    request.matchingRules.hasCategory('stuff')
  }

}
