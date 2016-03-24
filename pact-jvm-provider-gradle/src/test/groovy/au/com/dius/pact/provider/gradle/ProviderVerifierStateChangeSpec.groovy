package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.RequestResponsePact
import au.com.dius.pact.model.Response
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ProviderVerifierStateChangeSpec extends Specification {

  private ProviderVerifier providerVerifier
  private ProviderInfo providerInfo
  private Closure consumer
  private Project project
  private String state
  private makeStateChangeRequestArgs
  private final consumerMap = [name: 'bob']
  private mockProviderClient
  private Task otherTask
  private PactVerificationTask pactVerificationTask

  def setup() {
    state = 'there is a state'
    providerInfo = new ProviderInfo()
    consumer = { consumerMap as ConsumerInfo }
    project = ProjectBuilder.builder().build()
    providerVerifier = new ProviderVerifier()
    pactVerificationTask = project.task('verification', type: PactVerificationTask)
    makeStateChangeRequestArgs = []
    mockProviderClient = [
      makeStateChangeRequest: { arg1, arg2, arg3, arg4, arg5 ->
        makeStateChangeRequestArgs << [arg1, arg2, arg3, arg4, arg5]
        null
      },
      makeRequest: { [statusCode: 200, headers: [:], data: '{}', contentType: 'application/json'] }
    ] as ProviderClient
    ProviderClient.metaClass.constructor = { args -> mockProviderClient }
    otherTask = project.task('otherTask') {
      ext.providerState = ''
    }
  }

  def cleanup() {
    GroovySystem.metaClassRegistry.setMetaClass(ProviderClient, null)
  }

  def 'if the state change is null, does nothing'() {
    given:
    consumerMap.stateChange = null

    when:
    def result = providerVerifier.stateChange(state, providerInfo, consumer())

    then:
    result
    makeStateChangeRequestArgs == []
  }

  def 'if the state change is an empty string, does nothing'() {
    given:
    consumerMap.stateChange = ''

    when:
    def result = providerVerifier.stateChange(state, providerInfo, consumer())

    then:
    result
    makeStateChangeRequestArgs == []
  }

  def 'if the state change is a blank string, does nothing'() {
    given:
    consumerMap.stateChange = '      '

    when:
    def result = providerVerifier.stateChange(state, providerInfo, consumer())

    then:
    result
    makeStateChangeRequestArgs == []
  }

  def 'if the state change is a URL, performs a state change request'() {
    given:
    consumerMap.stateChange = 'http://localhost:2000/hello'

    when:
    def result = providerVerifier.stateChange(state, providerInfo, consumer())

    then:
    result
    makeStateChangeRequestArgs == [[new URI('http://localhost:2000/hello'), 'there is a state', true, true, false]]
  }

  def 'if the state change is a closure, executes it with the state change as a parameter'() {
    given:
    def closureArgs = []
    consumerMap.stateChange = { arg -> closureArgs << arg; true }

    when:
    def result = providerVerifier.stateChange(state, providerInfo, consumer())

    then:
    result
    makeStateChangeRequestArgs == []
    closureArgs == ['there is a state']
  }

//  @Test
//  void 'if the state change is a Gradle task, executes it in a sub-build'() {
//    def closureArgs = []
//    otherTask.doLast { closureArgs << it.providerState; println ">>> [$it.providerState]" }
//    consumerMap.stateChange = otherTask
//    providerVerifier.isBuildSpecificTask = { true }
//    providerVerifier.executeBuildSpecificTask = pactVerificationTask.&executeStateChangeTask
//    assert providerVerifier.stateChange(state, providerInfo, consumer()) == true
//    assert makeStateChangeRequestArgs == []
//    assert closureArgs == ['there is a state']
//  }

//  @Test
//  void 'if the state change is a string that names a gradle providerVerifier, executes it in a sub-build'() {
//    def closureArgs = []
//    otherTask.doLast { closureArgs << it.providerState; println ">>> [$it.providerState]" }
//    consumerMap.stateChange = 'otherTask'
//    assert providerVerifier.stateChange(state, providerInfo, consumer()) == true
//    assert makeStateChangeRequestArgs == []
//    assert closureArgs == ['there is a state']
//  }

  def 'if the state change is a string that is not handled by the other conditions, does nothing'() {
    given:
    consumerMap.stateChange = 'blah blah blah'

    when:
    def result = providerVerifier.stateChange(state, providerInfo, consumer())

    then:
    result
    makeStateChangeRequestArgs == []
  }

  def 'if teardown is set then a statechage teardown request is made after the test'() {
    given:
    def interaction = new RequestResponseInteraction('provider state test', 'state of the nation',
      new Request(), new Response(200, [:], OptionalBody.body('{}'), [:]))
    def pact = new RequestResponsePact(new Provider('Bob'), new Consumer('Bobbie'), [interaction])
    def failures = [:]
    consumerMap.stateChange = 'http://localhost:2000/hello'
    providerInfo.stateChangeTeardown = true

    when:
    providerVerifier.verifyInteraction(providerInfo, consumer(), pact, failures, interaction)

    then:
    makeStateChangeRequestArgs == [
      [new URI('http://localhost:2000/hello'), 'state of the nation', true, true, true],
      [new URI('http://localhost:2000/hello'), 'state of the nation', true, false, true]
    ]
  }

  def 'if the state change is a closure and teardown is set, executes it with the state change as a parameter'() {
    given:
    def closureArgs = []
    consumerMap.stateChange = { arg1, arg2 ->
      closureArgs << [arg1, arg2]
      true
    }
    def interaction = new RequestResponseInteraction('provider state test', 'state of the nation',
      new Request(), new Response(200, [:], OptionalBody.body('{}'), [:]))
    def pact = new RequestResponsePact(new Provider('Bob'), new Consumer('Bobbie'), [interaction])
    def failures = [:]
    providerInfo.stateChangeTeardown = true

    when:
    providerVerifier.verifyInteraction(providerInfo, consumer(), pact, failures, interaction)

    then:
    makeStateChangeRequestArgs == []
    closureArgs == [['state of the nation', 'setup'], ['state of the nation', 'teardown']]
  }

}
