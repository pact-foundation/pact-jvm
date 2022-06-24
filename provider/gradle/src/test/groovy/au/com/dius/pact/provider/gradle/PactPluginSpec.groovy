package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.support.Auth
import au.com.dius.pact.provider.PactVerification
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class PactPluginSpec extends Specification {

  private PactPlugin plugin
  private Project project

  void setup() {
    project = ProjectBuilder.builder().build()
    plugin = new PactPlugin()
    plugin.apply(project)
  }

  def 'defines a pactVerify task'() {
    expect:
    project.tasks.pactVerify
  }

  def 'defines a pactPublish task'() {
    expect:
    project.tasks.pactPublish
  }

  def 'defines a task for each defined provider'() {
    given:
    project.pact {
      serviceProviders {
        provider1 {

        }

        provider2 {

        }
      }
    }

    when:
    project.evaluate()

    then:
    project.tasks.pactVerify_provider1
    project.tasks.pactVerify_provider2
  }

  def 'replaces white space with underscores'() {
    given:
    project.pact {
      serviceProviders {
        'invalid Name' {

        }
      }
    }

    when:
    project.evaluate()

    then:
    project.tasks.pactVerify_invalid_Name
  }

  def 'defines a task for each file in the pact file directory'() {
    given:
    def resource = getClass().classLoader.getResource('pacts/foo_pact.json')
    File pactFileDirectory = new File(resource.file).parentFile
    project.pact {
      serviceProviders {
        provider1 {
          hasPactsWith('many consumers') {
            pactFileLocation = project.file("${pactFileDirectory.absolutePath}")
            stateChange = 'http://localhost:8080/state'
          }
        }
      }
    }

    when:
    project.evaluate()
    def consumers = project.tasks.pactVerify_provider1.providerToVerify.consumers

    then:
    consumers.size() == 2
    consumers.find { it.name == 'Foo Consumer' }
    consumers.find { it.name == 'Bar Consumer' }
  }

  def 'configures the providers and consumers correctly'() {
    given:
    def pactFileUrl = 'http://localhost:8000/pacts/provider/prividera/consumer/consumera/latest'
    def stateChangeUrl = 'http://localhost:8080/stateChange'
    project.pact {
      serviceProviders {
        ProviderA { providerInfo ->
          startProviderTask = 'jettyEclipseRun'
          terminateProviderTask = 'jettyEclipseStop'

          port = 1234

          hasPactWith('ConsumerA') {
            pactSource = pactFileUrl
            stateChange = stateChangeUrl
            verificationType = 'REQUEST_RESPONSE'
          }
        }
      }
    }

    when:
    project.evaluate()

    def provider = project.tasks.pactVerify_ProviderA.providerToVerify
    def consumer = provider.consumers.first()

    then:
    provider.startProviderTask == 'jettyEclipseRun'
    provider.terminateProviderTask == 'jettyEclipseStop'
    provider.port == 1234

    consumer.name == 'ConsumerA'
    consumer.pactSource == pactFileUrl
    consumer.stateChange == stateChangeUrl
    consumer.verificationType == PactVerification.REQUEST_RESPONSE
  }

  def 'do not set the state change url automatically'() {
    given:
    def pactFileUrl = 'http://localhost:8000/pacts/provider/prividera/consumer/consumera/latest'
    project.pact {
      serviceProviders {
        ProviderA { providerInfo ->
          hasPactWith('ConsumerA') {
            pactSource = url(pactFileUrl)
          }
        }
      }
    }

    when:
    project.evaluate()
    def consumer = project.tasks.pactVerify_ProviderA.providerToVerify.consumers.first()

    then:
    consumer.pactSource.toString() == pactFileUrl
    consumer.stateChange == null
  }

  def 'configures the publish task correctly'() {
    given:
    project.pact {

      serviceProviders {
        ProviderA {
          hasPactWith('ConsumerA') {

          }
        }
      }

      publish {
        pactDirectory = '/pact/dir'
        pactBrokerUrl = 'http://pactbroker:1234'
      }
    }

    when:
    project.evaluate()

    then:
    project.pact.publish.pactDirectory == '/pact/dir'
    project.pact.publish.pactBrokerUrl == 'http://pactbroker:1234'
  }

  def 'fails if there pact is not a valid configuration'() {
    given:
    project.ext.pact = '123'
    project.pact {
      serviceProviders {
        ProviderA {
          hasPactWith('ConsumerA') {

          }
        }
      }
    }

    when:
    project.evaluate()

    then:
    thrown(ProjectConfigurationException)
  }

  def 'hasPactWith - allows all the values for the consumer to be configured'() {
    given:
    project.pact {
      serviceProviders {
        ProviderA {
          hasPactWith('boddy the consumer') {
            stateChange = url('http://localhost:8001/tasks/pactStateChange')
            stateChangeUsesBody = false
            packagesToScan = ['one', 'two']
            verificationType = PactVerification.REQUEST_RESPONSE
            pactSource = project.file('path/to/pact')
          }
        }
      }
    }

    when:
    project.evaluate()
    def consumer = project.tasks.pactVerify_ProviderA.providerToVerify.consumers.first()

    then:
    consumer.name == 'boddy the consumer'
    consumer.auth == Auth.None.INSTANCE
    consumer.packagesToScan == ['one', 'two']
    consumer.pactSource instanceof File
    consumer.pactSource.toString().endsWith('path/to/pact')
    consumer.stateChange.toString() == 'http://localhost:8001/tasks/pactStateChange'
    !consumer.stateChangeUsesBody
  }
}
