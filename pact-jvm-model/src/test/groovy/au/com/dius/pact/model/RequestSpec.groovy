package au.com.dius.pact.model

import spock.lang.Specification

class RequestSpec extends Specification {

  def 'delegates to the matching rules to parse matchers'() {
    given:
    def json = [
      matchingRules: [
        'stuff': ['': [matchers: [ [match: 'type'] ] ] ]
      ]
    ]

    when:
    def request = Request.fromMap(json)

    then:
    !request.matchingRules.empty
    request.matchingRules.hasCategory('stuff')
  }

  def 'fromMap sets defaults for attributes missing from the map'() {
    expect:
    request.method == 'GET'
    request.path == '/'
    request.query.isEmpty()
    request.headers.isEmpty()
    request.body.isMissing()
    request.matchingRules.empty
    request.generators.empty

    where:
    request = Request.fromMap([:])
  }

}
