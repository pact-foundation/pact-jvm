package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProviderVerifierStateChangeTest {

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

  @Before
  void setup() {
    state = 'there is a state'
    providerInfo = new ProviderInfo()
    consumer = { consumerMap as ConsumerInfo }
    project = ProjectBuilder.builder().build()
    providerVerifier = new ProviderVerifier()
    pactVerificationTask = project.task('verification', type: PactVerificationTask)
    makeStateChangeRequestArgs = []
    mockProviderClient = [makeStateChangeRequest: { arg1, arg2, arg3 ->
      makeStateChangeRequestArgs << [arg1, arg2, arg3]
      null
    } ] as ProviderClient
    ProviderClient.metaClass.constructor = { args -> mockProviderClient }
    otherTask = project.task('otherTask') {
      ext.providerState = ''
    }
  }

  @After
  void cleanup() {
    GroovySystem.metaClassRegistry.setMetaClass(ProviderClient, null)
  }

  @Test
  void 'if the state change is null, does nothing'() {
    consumerMap.stateChange = null
    assert providerVerifier.stateChange(state, providerInfo, consumer()) == true
    assert makeStateChangeRequestArgs == []
  }

  @Test
  void 'if the state change is an empty string, does nothing'() {
    consumerMap.stateChange = ''
    assert providerVerifier.stateChange(state, providerInfo, consumer()) == true
    assert makeStateChangeRequestArgs == []
  }

  @Test
  void 'if the state change is a blank string, does nothing'() {
    consumerMap.stateChange = '      '
    assert providerVerifier.stateChange(state, providerInfo, consumer()) == true
    assert makeStateChangeRequestArgs == []
  }

  @Test
  void 'if the state change is a URL, performs a state change request'() {
    consumerMap.stateChange = 'http://localhost:2000/hello'
    assert providerVerifier.stateChange(state, providerInfo, consumer()) == true
    assert makeStateChangeRequestArgs == [[new URI('http://localhost:2000/hello'), 'there is a state', true]]
  }

  @Test
  void 'if the state change is a closure, executes it with the state change as a parameter'() {
    def closureArgs = []
    consumerMap.stateChange = { arg -> closureArgs << arg; true }
    assert providerVerifier.stateChange(state, providerInfo, consumer()) == true
    assert makeStateChangeRequestArgs == []
    assert closureArgs == ['there is a state']
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

  @Test
  void 'if the state change is a string that is not handled by the other conditions, does nothing'() {
    consumerMap.stateChange = 'blah blah blah'
    assert providerVerifier.stateChange(state, providerInfo, consumer()) == true
    assert makeStateChangeRequestArgs == []
  }

}
