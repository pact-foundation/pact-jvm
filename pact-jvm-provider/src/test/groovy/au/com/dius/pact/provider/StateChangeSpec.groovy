package au.com.dius.pact.provider

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.ProviderState
import spock.lang.Specification

class StateChangeSpec extends Specification {

  private ProviderVerifier providerVerifier
  private ProviderInfo providerInfo
  private Closure consumer
  private ProviderState state
  private makeStateChangeRequestArgs
  private final consumerMap = [name: 'bob']
  private mockProviderClient

  def setup() {
    state = new ProviderState('there is a state')
    providerInfo = new ProviderInfo()
    consumer = { consumerMap as ConsumerInfo }
    providerVerifier = new ProviderVerifier()
    makeStateChangeRequestArgs = []
    mockProviderClient = [
      makeStateChangeRequest: { arg1, arg2, arg3, arg4, arg5 ->
        makeStateChangeRequestArgs << [arg1, arg2, arg3, arg4, arg5]
        null
      },
      makeRequest: { [statusCode: 200, headers: [:], data: '{}', contentType: 'application/json'] }
    ] as ProviderClient
    ProviderClient.metaClass.constructor = { args -> mockProviderClient }
  }

  def cleanup() {
    GroovySystem.metaClassRegistry.setMetaClass(ProviderClient, null)
  }

  def 'if the state change is null, does nothing'() {
    given:
    consumerMap.stateChange = null

    when:
    def result = StateChange.stateChange(providerVerifier, state, providerInfo, consumer(), true)

    then:
    result
    makeStateChangeRequestArgs == []
  }

  def 'if the state change is an empty string, does nothing'() {
    given:
    consumerMap.stateChange = ''

    when:
    def result = StateChange.stateChange(providerVerifier, state, providerInfo, consumer(), true)

    then:
    result
    makeStateChangeRequestArgs == []
  }

  def 'if the state change is a blank string, does nothing'() {
    given:
    consumerMap.stateChange = '      '

    when:
    def result = StateChange.stateChange(providerVerifier, state, providerInfo, consumer(), true)

    then:
    result
    makeStateChangeRequestArgs == []
  }

  def 'if the state change is a URL, performs a state change request'() {
    given:
    consumerMap.stateChange = 'http://localhost:2000/hello'

    when:
    def result = StateChange.stateChange(providerVerifier, state, providerInfo, consumer(), true)

    then:
    result
    makeStateChangeRequestArgs == [
      [new URI('http://localhost:2000/hello'), state, true, true, false]
    ]
  }

  def 'if the state change is a closure, executes it with the state change as a parameter'() {
    given:
    def closureArgs = []
    consumerMap.stateChange = { arg -> closureArgs << arg; true }

    when:
    def result = StateChange.stateChange(providerVerifier, state, providerInfo, consumer(), true)

    then:
    result
    makeStateChangeRequestArgs == []
    closureArgs == [state]
  }

  def 'if the state change is a string that is not handled by the other conditions, does nothing'() {
    given:
    consumerMap.stateChange = 'blah blah blah'

    when:
    def result = StateChange.stateChange(providerVerifier, state, providerInfo, consumer(), true)

    then:
    result
    makeStateChangeRequestArgs == []
  }

  def 'if there is more than one state, performs a state change request for each'() {
    given:
    consumerMap.stateChange = 'http://localhost:2000/hello'
    def stateOne = new ProviderState('one', [a: 'b', c: 'd'])
    def stateTwo = new ProviderState('two', [a: 1, c: 2])
    def interaction = [
      getProviderStates: { [stateOne, stateTwo] }
    ] as Interaction

    when:
    def result = StateChange.executeStateChange(providerVerifier, providerInfo, consumer(), interaction, '', [:])

    then:
    result.stateChangeOk
    result.message == ' Given one And two'
    makeStateChangeRequestArgs == [
      [new URI('http://localhost:2000/hello'), stateOne, true, true, false],
      [new URI('http://localhost:2000/hello'), stateTwo, true, true, false]
    ]
  }
}
