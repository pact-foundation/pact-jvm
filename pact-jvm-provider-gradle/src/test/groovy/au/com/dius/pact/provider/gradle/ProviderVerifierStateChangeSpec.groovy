package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.RequestResponsePact
import au.com.dius.pact.model.Response
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import spock.lang.Specification

class ProviderVerifierStateChangeSpec extends Specification {

  private ProviderVerifier providerVerifier
  private ProviderInfo providerInfo
  private Closure consumer
  private makeStateChangeRequestArgs
  private final consumerMap = [name: 'bob']
  private mockProviderClient

  def setup() {
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

  def 'if teardown is set then a statechage teardown request is made after the test'() {
    def state = new ProviderState('state of the nation')
    given:
    def interaction = new RequestResponseInteraction('provider state test', [state],
      new Request(), new Response(200, [:], OptionalBody.body('{}'), [:]))
    def pact = new RequestResponsePact(new Provider('Bob'), new Consumer('Bobbie'), [interaction])
    def failures = [:]
    consumerMap.stateChange = 'http://localhost:2000/hello'
    providerInfo.stateChangeTeardown = true

    when:
    providerVerifier.verifyInteraction(providerInfo, consumer(), pact, failures, interaction)

    then:
    makeStateChangeRequestArgs == [
      [new URI('http://localhost:2000/hello'), state, true, true, true],
      [new URI('http://localhost:2000/hello'), state, true, false, true]
    ]
  }

  def 'if the state change is a closure and teardown is set, executes it with the state change as a parameter'() {
    given:
    def closureArgs = []
    consumerMap.stateChange = { arg1, arg2 ->
      closureArgs << [arg1, arg2]
      true
    }
    def state = new ProviderState('state of the nation')
    def interaction = new RequestResponseInteraction('provider state test', [state],
      new Request(), new Response(200, [:], OptionalBody.body('{}'), [:]))
    def pact = new RequestResponsePact(new Provider('Bob'), new Consumer('Bobbie'), [interaction])
    def failures = [:]
    providerInfo.stateChangeTeardown = true

    when:
    providerVerifier.verifyInteraction(providerInfo, consumer(), pact, failures, interaction)

    then:
    makeStateChangeRequestArgs == []
    closureArgs == [[state, 'setup'], [state, 'teardown']]
  }

}
