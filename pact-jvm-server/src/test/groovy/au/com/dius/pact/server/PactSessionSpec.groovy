package au.com.dius.pact.server

import au.com.dius.pact.core.matchers.PartialRequestMatch
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import spock.lang.Specification

class PactSessionSpec extends Specification {
  @SuppressWarnings('LineLength')
  def 'invalid request returns JSON response with details about the request'() {
    given:
    def session = PactSession.empty

    when:
    def response = session.invalidRequest(new Request('GET', '/test'))

    then:
    response.status == 500
    response.body.valueAsString() == '{ "error": "Unexpected request : \\tmethod: GET\\n\\tpath: \\/test\\n\\tquery: {}\\n\\theaders: {}\\n\\tmatchers: MatchingRules(rules={})\\n\\tgenerators: Generators(categories={})\\n\\tbody: MISSING" }'
  }

  def 'receive request records the match against the expected request'() {
    given:
    def interaction = new RequestResponseInteraction('test', [], new Request('GET', '/test'))
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction ])
    def session = PactSession.forPact(pact)
    def request = new Request('GET', '/test')

    when:
    def response = session.receiveRequest(request)

    then:
    response.first.status == 200
    response.second.results.allMatched()
  }

  def 'record unexpected adds an unexpected request to the session'() {
    given:
    def session = PactSession.empty
    def request = new Request('GET', '/test')

    when:
    def result = session.recordUnexpected(request)

    then:
    !result.results.allMatched()
    result.results.unexpected == [ request ]
  }

  def 'record almost matched adds a match result to the session'() {
    given:
    def session = PactSession.empty
    def matchResult = new PartialRequestMatch([:])

    when:
    def result = session.recordAlmostMatched(matchResult)

    then:
    result.results.allMatched()
    result.results.almostMatched == [ matchResult ]
  }

  def 'record matched adds the interaction to the session'() {
    given:
    def session = PactSession.empty
    def interaction = new RequestResponseInteraction('test', [], new Request('GET', '/test'))

    when:
    def result = session.recordMatched(interaction)

    then:
    result.results.allMatched()
    result.results.matched == [ interaction ]
  }

  def 'remaining results returns any unmatched interactions as missing'() {
    given:
    def interaction1 = new RequestResponseInteraction('test', [], new Request('GET', '/test'))
    def interaction2 = new RequestResponseInteraction('test2', [], new Request('GET', '/test2'))
    def interaction3 = new RequestResponseInteraction('test3', [], new Request('GET', '/test3'))
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2, interaction3 ])
    def session = PactSession.forPact(pact)

    when:
    session = session.recordMatched(interaction1)
    session = session.recordMatched(interaction3)
    def result = session.remainingResults()

    then:
    !result.allMatched()
    result.missing == [ interaction2 ]
  }
}
