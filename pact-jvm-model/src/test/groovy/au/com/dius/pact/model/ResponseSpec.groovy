package au.com.dius.pact.model

import spock.lang.Specification

class ResponseSpec extends Specification {

  def 'delegates to the matching rules to parse matchers'() {
    given:
    def json = [
      matchingRules: [
        'stuff': [:]
      ]
    ]

    when:
    def response = Response.fromMap(json)

    then:
    !response.matchingRules.empty
    response.matchingRules.hasCategory('stuff')
  }

}
