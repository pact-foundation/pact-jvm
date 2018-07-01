package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.UrlSource
import au.com.dius.pact.provider.PactVerification
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

class PactPluginTest {

    private PactPlugin plugin
    private Project project

    @Before
    void setup() {
        project = ProjectBuilder.builder().build()
        plugin = new PactPlugin()
        plugin.apply(project)
    }

    @Test
    void 'defines a pactVerify task'() {
        assert project.tasks.pactVerify
    }

    @Test
    void 'defines a pactPublish task'() {
        assert project.tasks.pactPublish
    }

    @Test
    void 'defines a task for each defined provider'() {
        project.pact {
            serviceProviders {
                provider1 {

                }

                provider2 {

                }
            }
        }

        project.evaluate()

        assert project.tasks.pactVerify_provider1
        assert project.tasks.pactVerify_provider2
    }

    @Test
    void 'replaces white space with underscores'() {
        project.pact {
            serviceProviders {
                'invalid Name' {

                }
            }
        }
        project.evaluate()

        assert project.tasks.pactVerify_invalid_Name
    }

    @Test
    void 'defines a task for each file in the pact file directory'() {
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
        project.evaluate()

        def consumers = project.tasks.pactVerify_provider1.providerToVerify.consumers
        assert consumers.size() == 2
        assert consumers.find { it.name == 'Foo Consumer' }
        assert consumers.find { it.name == 'Bar Consumer' }
    }

    @Test
    void 'configures the providers and consumers correctly'() {
        def pactFileUrl = 'http://localhost:8000/pacts/provider/prividera/consumer/consumera/latest'
        def stateChangeUrl = 'http://localhost:8080/stateChange'
        project.pact {
            serviceProviders {
                ProviderA { providerInfo ->
                    startProviderTask = 'jettyEclipseRun'
                    terminateProviderTask = 'jettyEclipseStop'

                    port = 1234

                    hasPactWith('ConsumerA') {
                        pactSource = url(pactFileUrl)
                        stateChange = url(stateChangeUrl)
                        verificationType = 'REQUST_RESPONSE'
                    }
                }
            }
        }

        project.evaluate()

        def provider = project.tasks.pactVerify_ProviderA.providerToVerify
        assert provider.startProviderTask == 'jettyEclipseRun'
        assert provider.terminateProviderTask == 'jettyEclipseStop'
        assert provider.port == 1234

        def consumer = provider.consumers.first()
        assert consumer.name == 'ConsumerA'
        assert consumer.pactSource == new UrlSource(pactFileUrl)
        assert consumer.stateChange == new UrlSource(stateChangeUrl)
        assert consumer.verificationType == PactVerification.REQUST_RESPONSE
    }

    @Test
    void 'do not set the state change url automatically'() {
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

        project.evaluate()

        def consumer = project.tasks.pactVerify_ProviderA.providerToVerify.consumers.first()
        assert consumer.pactSource == new UrlSource(pactFileUrl)
        assert consumer.stateChange == null
    }

    @Test
    void 'configures the publish task correctly'() {
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

        project.evaluate()

        assert project.pact.publish.pactDirectory == '/pact/dir'
        assert project.pact.publish.pactBrokerUrl == 'http://pactbroker:1234'
    }

    @Test(expected = ProjectConfigurationException)
    void 'fails if there pact is not a valid configuration'() {
      project.ext.pact = '123'
      project.pact {
        serviceProviders {
          ProviderA {
            hasPactWith('ConsumerA') {

            }
          }
        }
      }

      project.evaluate()
    }

  @Test(expected = ProjectConfigurationException)
  void 'fails if there are no configured service providers and pactVerify is in the start parameters'() {
      project.pact {
          serviceProviders {
          }
      }
      project.gradle.startParameter.taskNames = ['pactVerify']

      project.evaluate()
  }

  @Test(expected = ProjectConfigurationException)
  void 'when checking for pactVerify in the start parameters, do a case insensitive check'() {
    project.pact {
      serviceProviders {
      }
    }
    project.gradle.startParameter.taskNames = ['pactverify']

    project.evaluate()
  }
}
