package au.com.dius.pact.provider.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test
@SuppressWarnings('UnusedImport')
import scala.None$
import scala.Some

class PactVerificationTaskTest {

  private PactVerificationTask task
  private consumer
  private interaction
  private Project project

  @Before
  void setup() {
    project = ProjectBuilder.builder().build()
    task = project.task('verification', type: PactVerificationTask)
  }

  @Test
  void 'if no consumer filter is defined, returns true'() {
    assert task.filterConsumers(consumer)
  }

  @Test
  void 'if a consumer filter is defined, returns false if the consumer name does not match'() {
    consumer = [name: 'bob']
    project.extensions.extraProperties.set('pact.filter.consumers', 'fred,joe')
    assert !task.filterConsumers(consumer)
  }

  @Test
  void 'if a consumer filter is defined, returns true if the consumer name does match'() {
    consumer = [name: 'bob']
    project.extensions.extraProperties.set('pact.filter.consumers', 'fred,joe,bob')
    assert task.filterConsumers(consumer)
  }

  @Test
  void 'trims whitespaces off the consumer names'() {
    consumer = [name: 'bob']
    project.extensions.extraProperties.set('pact.filter.consumers', 'fred,\tjoe, bob\n')
    assert task.filterConsumers(consumer)
  }

  @Test
  void 'if no interaction filter is defined, returns true'() {
    assert task.filterInteractions(interaction)
  }

  @Test
  void 'if an interaction filter is defined, returns false if the interaction description does not match'() {
    interaction = [description: { 'bob' } ]
    project.extensions.extraProperties.set('pact.filter.description', 'fred')
    assert !task.filterInteractions(interaction)
  }

  @Test
  void 'if an interaction filter is defined, returns true if the interaction description does match'() {
    interaction = [description: { 'bob' } ]
    project.extensions.extraProperties.set('pact.filter.description', 'bob')
    assert task.filterInteractions(interaction)
  }

  @Test
  void 'uses regexs to match the description'() {
    interaction = [description: { 'bobby' } ]
    project.extensions.extraProperties.set('pact.filter.description', 'bob.*')
    assert task.filterInteractions(interaction)
  }

  @Test
  void 'if no state filter is defined, returns true'() {
    assert task.filterInteractions(interaction)
  }

  @Test
  void 'if a state filter is defined, returns false if the interaction state does not match'() {
    interaction = [providerState: { Some.apply('bob') } ]
    project.extensions.extraProperties.set('pact.filter.providerState', 'fred')
    assert !task.filterInteractions(interaction)
  }

  @Test
  void 'if a state filter is defined, returns true if the interaction state does match'() {
    interaction = [providerState: { Some.apply('bob') } ]
    project.extensions.extraProperties.set('pact.filter.providerState', 'bob')
    assert task.filterInteractions(interaction)
  }

  @Test
  void 'uses regexs to match the state'() {
    interaction = [providerState: { Some.apply('bobby') } ]
    project.extensions.extraProperties.set('pact.filter.providerState', 'bob.*')
    assert task.filterInteractions(interaction)
  }

  @Test
  void 'if the state filter is empty, returns false if the interaction state is defined'() {
    interaction = [providerState: { Some.apply('bob') } ]
    project.extensions.extraProperties.set('pact.filter.providerState', '')
    assert !task.filterInteractions(interaction)
  }

  @Test
  void 'if the state filter is empty, returns true if the interaction state is not defined'() {
    interaction = [providerState: { None$.empty() } ]
    project.extensions.extraProperties.set('pact.filter.providerState', '')
    assert task.filterInteractions(interaction)
  }

  @Test
  void 'if the state filter and interaction filter is defined, must match both'() {
    interaction = [providerState: { Some.apply('bobby') }, description: { 'freddy' } ]
    project.extensions.extraProperties.set('pact.filter.description', '.*ddy')
    project.extensions.extraProperties.set('pact.filter.providerState', 'bob.*')
    assert task.filterInteractions(interaction)
  }

  @Test
  void 'if the state filter and interaction filter is defined, is false if description does not match'() {
    project.extensions.extraProperties.set('pact.filter.description', 'bob.*')
    project.extensions.extraProperties.set('pact.filter.providerState', '.*ddy')
    interaction = [providerState: { Some.apply('boddy') }, description: { 'freddy' } ]
    assert !task.filterInteractions(interaction)
  }

  @Test
  void 'if the state filter and interaction filter is defined, is false if state does not match'() {
    project.extensions.extraProperties.set('pact.filter.description', 'bob.*')
    project.extensions.extraProperties.set('pact.filter.providerState', '.*ddy')
    interaction = [providerState: { Some.apply('bobby') }, description: { 'frebby' } ]
    assert !task.filterInteractions(interaction)

    interaction = [providerState: { Some.apply('joe') }, description: { 'authur' } ]
    assert !task.filterInteractions(interaction)
  }

  @Test
  void 'if the state filter and interaction filter is defined, is false if both do not match'() {
    project.extensions.extraProperties.set('pact.filter.description', 'bob.*')
    project.extensions.extraProperties.set('pact.filter.providerState', '.*ddy')
    interaction = [providerState: { Some.apply('joe') }, description: { 'authur' } ]
    assert !task.filterInteractions(interaction)
  }

}
