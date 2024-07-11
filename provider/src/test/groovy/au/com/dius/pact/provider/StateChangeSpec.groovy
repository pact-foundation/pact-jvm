package au.com.dius.pact.provider

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.support.Result
import groovy.json.JsonOutput
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.HttpEntity
import spock.lang.Specification

@SuppressWarnings('PrivateFieldCouldBeFinal')
class StateChangeSpec extends Specification {

  private ProviderVerifier providerVerifier
  private ProviderInfo providerInfo
  private Closure consumer
  private ProviderState state
  private makeStateChangeRequestArgs, stateChangeResponse
  private Map consumerMap = [name: 'bob']
  private ProviderClient mockProviderClient

  def setup() {
    state = new ProviderState('there is a state')
    providerInfo = new ProviderInfo()
    consumer = { consumerMap as ConsumerInfo }
    providerVerifier = new ProviderVerifier()
    makeStateChangeRequestArgs = []
    stateChangeResponse = null
    mockProviderClient = Mock(ProviderClient) {
      makeStateChangeRequest(_, _, _, _, _) >> { args ->
        makeStateChangeRequestArgs << args
        stateChangeResponse
      }
      makeRequest(_) >> new ProviderResponse(200, [:], ContentType.JSON, OptionalBody.body('{}', ContentType.JSON))
    }
  }

  def 'if the state change is null, does nothing'() {
    given:
    consumerMap.stateChange = null

    when:
    def result = DefaultStateChange.INSTANCE.stateChange(providerVerifier, state, providerInfo, consumer(), true,
      mockProviderClient)

    then:
    result instanceof Result.Ok
    makeStateChangeRequestArgs == []
  }

  def 'if the state change is an empty string, does nothing'() {
    given:
    consumerMap.stateChange = ''

    when:
    def result = DefaultStateChange.INSTANCE.stateChange(providerVerifier, state, providerInfo, consumer(), true,
      mockProviderClient)

    then:
    result instanceof Result.Ok
    makeStateChangeRequestArgs == []
  }

  def 'if the state change is a blank string, does nothing'() {
    given:
    consumerMap.stateChange = '      '

    when:
    def result = DefaultStateChange.INSTANCE.stateChange(providerVerifier, state, providerInfo, consumer(), true,
      mockProviderClient)

    then:
    result instanceof Result.Ok
    makeStateChangeRequestArgs == []
  }

  def 'if the state change is a URL, performs a state change request'() {
    given:
    consumerMap.stateChange = 'http://localhost:2000/hello'

    when:
    def result = DefaultStateChange.INSTANCE.stateChange(providerVerifier, state, providerInfo, consumer(), true,
      mockProviderClient)

    then:
    result instanceof Result.Ok
    makeStateChangeRequestArgs == [
      [new URI('http://localhost:2000/hello'), state, true, true, false]
    ]
  }

  def 'Handle the case were the state change response has no body'() {
    given:
    consumerMap.stateChange = 'http://localhost:2000/hello'
    def entity = [getContentType: { null }] as HttpEntity
    stateChangeResponse = [
      getEntity: { entity },
      getCode: { 200 },
      close: { }
    ] as ClassicHttpResponse

    when:
    def result = DefaultStateChange.INSTANCE.stateChange(providerVerifier, state, providerInfo, consumer(), true,
      mockProviderClient)

    then:
    result instanceof Result.Ok
  }

  def 'if the state change is a closure, executes it with the state change as a parameter'() {
    given:
    def closureArgs = []
    consumerMap.stateChange = { arg -> closureArgs << arg; true }

    when:
    def result = DefaultStateChange.INSTANCE.stateChange(providerVerifier, state, providerInfo, consumer(), true,
      mockProviderClient)

    then:
    result instanceof Result.Ok
    makeStateChangeRequestArgs == []
    closureArgs == [state]
  }

  def 'if the state change is a closure, returns the result from the closure if it is a Map'() {
    given:
    def value = [
      a: 100,
      b: '200'
    ]
    consumerMap.stateChange = { arg -> value }

    when:
    def result = DefaultStateChange.INSTANCE.stateChange(providerVerifier, state, providerInfo, consumer(), true,
      mockProviderClient)

    then:
    result instanceof Result.Ok
    result.value == [a: 100, b: '200']
  }

  def 'if the state change is a closure, falls back to the state change parameters for state change results'() {
    given:
    def value = [
      a: 100,
      b: '200'
    ]
    consumerMap.stateChange = { arg -> value }
    state = new ProviderState('there is a state', [a: 1, b: 2, c: 'test'])

    when:
    def result = DefaultStateChange.INSTANCE.stateChange(providerVerifier, state, providerInfo, consumer(), true,
      mockProviderClient)

    then:
    result instanceof Result.Ok
    result.value == [a: 100, b: '200', c: 'test']
  }

  def 'if the state change is a string that is not handled by the other conditions, does nothing'() {
    given:
    consumerMap.stateChange = 'blah blah blah'

    when:
    def result = DefaultStateChange.INSTANCE.stateChange(providerVerifier, state, providerInfo, consumer(), true,
      mockProviderClient)

    then:
    result instanceof Result.Ok
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
    def result = DefaultStateChange.INSTANCE.executeStateChange(providerVerifier, providerInfo, consumer(), interaction,
      '', [:], mockProviderClient)

    then:
    result.stateChangeResult instanceof Result.Ok
    result.message == ' Given one And two'
    makeStateChangeRequestArgs == [
      [new URI('http://localhost:2000/hello'), stateOne, true, true, false],
      [new URI('http://localhost:2000/hello'), stateTwo, true, true, false]
    ]
  }

  def 'returns the result of the state change call if the result can be converted to a Map'() {
    given:
    consumerMap.stateChange = 'http://localhost:2000/state-change'
    def stateResult = JsonOutput.toJson([
      a: 100,
      b: '200'
    ])
    def entity = [
      getContentType: { 'application/json' },
      getContentLength: { stateResult.bytes.length as long },
      getContent: { new ByteArrayInputStream(stateResult.bytes) }
    ] as HttpEntity
    stateChangeResponse = [
            getEntity: { entity },
            getCode: { 200 },
            close: { }
    ] as ClassicHttpResponse

    when:
    def result = DefaultStateChange.INSTANCE.stateChange(providerVerifier, state, providerInfo, consumer(), true,
            mockProviderClient)

    then:
    result instanceof Result.Ok
    result.value == [a: 100, b: '200']
  }

  def 'falls back to the state change parameters for state change results'() {
    given:
    consumerMap.stateChange = 'http://localhost:2000/state-change'
    def stateResult = JsonOutput.toJson([
      a: 100,
      b: '200'
    ])
    def entity = [
      getContentType: { 'application/json' },
      getContentLength: { stateResult.bytes.length as long },
      getContent: { new ByteArrayInputStream(stateResult.bytes) }
    ] as HttpEntity
    stateChangeResponse = [
      getEntity: { entity },
      getCode: { 200 },
      close: { }
    ] as ClassicHttpResponse
    state = new ProviderState('there is a state', [a: 1, b: 2, c: 'test'])

    when:
    def result = DefaultStateChange.INSTANCE.stateChange(providerVerifier, state, providerInfo, consumer(), true,
      mockProviderClient)

    then:
    result instanceof Result.Ok
    result.value == [a: 100, b: '200', c: 'test']
  }
}
