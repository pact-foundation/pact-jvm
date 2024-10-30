package au.com.dius.pact.server

import au.com.dius.pact.core.matchers.PartialRequestMatch
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import spock.lang.Specification

class PactSessionResultsSpec extends Specification {
  def 'empty state'() {
    given:
    def state = PactSessionResults.empty

    expect:
    state.allMatched()
    state.almostMatched.empty
    state.matched.empty
    state.unexpected.empty
    state.missing.empty
  }

  def 'add matched interaction'() {
    given:
    def state = PactSessionResults.empty

    when:
    state = state.addMatched(new RequestResponseInteraction('test'))

    then:
    state.allMatched()
    state.almostMatched.empty
    state.matched*.description == ['test']
    state.unexpected.empty
    state.missing.empty
  }

  def 'add two matched interactions'() {
    given:
    def state = PactSessionResults.empty

    when:
    state = state.addMatched(new RequestResponseInteraction('test'))
    state = state.addMatched(new RequestResponseInteraction('test2'))

    then:
    state.allMatched()
    state.almostMatched.empty
    state.matched*.description == ['test2', 'test']
    state.unexpected.empty
    state.missing.empty
  }

  def 'add unexpected request'() {
    given:
    def state = PactSessionResults.empty
    def request = new Request('GET', '/test')

    when:
    state = state.addUnexpected(request)

    then:
    !state.allMatched()
    state.almostMatched.empty
    state.matched.empty
    state.unexpected*.path == ['/test']
    state.missing.empty
  }

  def 'add missing interactions'() {
    given:
    def state = PactSessionResults.empty
    def interaction1 = new RequestResponseInteraction('test')
    def interaction2 = new RequestResponseInteraction('test2')
    def interaction3 = new RequestResponseInteraction('test3')

    when:
    state = state.addMissing([interaction1])
    state = state.addMissing([interaction2, interaction3])

    then:
    !state.allMatched()
    state.almostMatched.empty
    state.matched.empty
    state.unexpected.empty
    state.missing*.description == ['test2', 'test3', 'test']
  }

  def 'add almost matched'() {
    given:
    def state = PactSessionResults.empty

    when:
    state = state.addAlmostMatched(new PartialRequestMatch([:]))

    then:
    state.allMatched()
    !state.almostMatched.empty
    state.matched.empty
    state.unexpected.empty
    state.missing.empty
  }
}
