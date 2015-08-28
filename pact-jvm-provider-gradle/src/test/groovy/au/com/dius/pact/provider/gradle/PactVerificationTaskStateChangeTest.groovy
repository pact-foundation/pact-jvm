package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderClient
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

class PactVerificationTaskStateChangeTest {

  private PactVerificationTask task
  private Closure consumer
  private Project project
  private String state
  private makeStateChangeRequestArgs
  private final consumerMap = [name: 'bob']
  private mockProviderClient
  private Task otherTask

  @Before
  void setup() {
    state = 'there is a state'
    consumer = { consumerMap as ConsumerInfo }
    project = ProjectBuilder.builder().build()
    task = project.task('verification', type: PactVerificationTask)
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
    assert task.stateChange(state, consumer()) == true
    assert makeStateChangeRequestArgs == []
  }

  @Test
  void 'if the state change is an empty string, does nothing'() {
    consumerMap.stateChange = ''
    assert task.stateChange(state, consumer()) == true
    assert makeStateChangeRequestArgs == []
  }

  @Test
  void 'if the state change is a blank string, does nothing'() {
    consumerMap.stateChange = '      '
    assert task.stateChange(state, consumer()) == true
    assert makeStateChangeRequestArgs == []
  }

  @Test
  void 'if the state change is a URL, performs a state change request'() {
    consumerMap.stateChange = 'http://localhost:2000/hello'
    assert task.stateChange(state, consumer()) == true
    assert makeStateChangeRequestArgs == [[new URI('http://localhost:2000/hello'), 'there is a state', true]]
  }

  @Test
  void 'if the state change is a closure, executes it with the state change as a parameter'() {
    def closureArgs = []
    consumerMap.stateChange = { arg -> closureArgs << arg; true }
    assert task.stateChange(state, consumer()) == true
    assert makeStateChangeRequestArgs == []
    assert closureArgs == ['there is a state']
  }

//  @Test
//  void 'if the state change is a Gradle task, executes it in a sub-build'() {
//    def closureArgs = []
//    otherTask.doLast { closureArgs << it.providerState; println ">>> [$it.providerState]" }
//    consumerMap.stateChange = otherTask
//    assert task.stateChange(state, consumer()) == true
//    assert makeStateChangeRequestArgs == []
//    assert closureArgs == ['there is a state']
//  }
//
//  @Test
//  void 'if the state change is a string that names a gradle task, executes it in a sub-build'() {
//    def closureArgs = []
//    otherTask.doLast { closureArgs << it.providerState; println ">>> [$it.providerState]" }
//    consumerMap.stateChange = 'otherTask'
//    assert task.stateChange(state, consumer()) == true
//    assert makeStateChangeRequestArgs == []
//    assert closureArgs == ['there is a state']
//  }

  @Test
  void 'if the state change is a string that is not handled by the other conditions, does nothing'() {
    consumerMap.stateChange = 'blah blah blah'
    assert task.stateChange(state, consumer()) == true
    assert makeStateChangeRequestArgs == []
  }

}
