package au.com.dius.pact.model

import spock.lang.Specification
import spock.lang.Unroll

class ResponseSpec extends Specification {

  def 'delegates to the matching rules to parse matchers'() {
    given:
    def json = [
      matchingRules: [
        'stuff': ['': [matchers: [ [match: 'type'] ] ] ]
      ]
    ]

    when:
    def response = Response.fromMap(json)

    then:
    !response.matchingRules.empty
    response.matchingRules.hasCategory('stuff')
  }

  def 'fromMap sets defaults for attributes missing from the map'() {
    expect:
    response.status == 200
    response.headers.isEmpty()
    response.body.isMissing()
    response.matchingRules.empty
    response.generators.empty

    where:
    response = Response.fromMap([:])
  }

  @Unroll
  def 'fromMap should handle different number types'() {
    expect:
    Response.fromMap([status: statusValue]).status == 200

    where:
    statusValue << [200, 200.0, new BigDecimal('200'), new BigInteger('200')]
  }

}
