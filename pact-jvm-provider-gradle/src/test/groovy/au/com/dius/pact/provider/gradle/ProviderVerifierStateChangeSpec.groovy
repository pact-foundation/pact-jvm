package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.StateChange
import spock.lang.Specification

class ProviderVerifierStateChangeSpec extends Specification {

  private ProviderVerifier providerVerifier
  private ProviderInfo providerInfo
  private ConsumerInfo consumer
  private ProviderClient providerClient

  def setup() {
    providerInfo = new ProviderInfo()
    consumer = new ConsumerInfo(name: 'Bob')
    providerVerifier = new ProviderVerifier()
    providerClient = Mock()
  }

  def 'if teardown is set then a statechage teardown request is made after the test'() {
    given:
    def state = new ProviderState('state of the nation')
    def interaction = new RequestResponseInteraction('provider state test', [state],
      new Request(), new Response(200, [:], OptionalBody.body('{}')))
    def failures = [:]
    consumer.stateChange = 'http://localhost:2000/hello'
    providerInfo.stateChangeTeardown = true
    GroovyMock(StateChange, global: true)

    when:
    providerVerifier.verifyInteraction(providerInfo, consumer, failures, interaction)

    then:
    1 * StateChange.executeStateChange(*_) >> new StateChange.StateChangeResult(true, 'interactionMessage')
    1 * StateChange.executeStateChangeTeardown(providerVerifier, interaction, providerInfo, consumer, _)
  }

  def 'if the state change is a closure and teardown is set, executes it with the state change as a parameter'() {
    given:
    def closureArgs = []
    consumer.stateChange = { arg1, arg2 ->
      closureArgs << [arg1, arg2]
      true
    }
    def state = new ProviderState('state of the nation')
    def interaction = new RequestResponseInteraction('provider state test', [state],
      new Request(), new Response(200, [:], OptionalBody.body('{}')))
    def failures = [:]
    providerInfo.stateChangeTeardown = true

    when:
    StateChange.executeStateChange(providerVerifier, providerInfo, consumer, interaction, 'state of the nation',
      failures, providerClient)
    StateChange.executeStateChangeTeardown(providerVerifier, interaction, providerInfo, consumer, providerClient)

    then:
    closureArgs == [[state, 'setup'], [state, 'teardown']]
  }

}
