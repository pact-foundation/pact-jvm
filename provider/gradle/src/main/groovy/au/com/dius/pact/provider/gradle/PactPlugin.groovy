package au.com.dius.pact.provider.gradle

import groovy.transform.CompileStatic
import org.gradle.api.GradleScriptException
import org.gradle.api.Project

/**
 * Main plugin class
 */
@SuppressWarnings('AbcMetric')
class PactPlugin extends PactPluginBase {

    @Override
    @SuppressWarnings('MethodSize')
    void apply(Project project) {

        // Create and install the extension object
        def extension = project.extensions.create('pact', PactPluginExtension,
          project.container(GradleProviderInfo))

        project.task(PACT_VERIFY, description: 'Verify your pacts against your providers', group: GROUP)

        project.tasks.register('pactPublish', PactPublishTask) {
          group = GROUP
          description = 'Publish your pacts to a pact broker'
          pactPublish.set(extension.publish)
          broker.set(extension.broker)
          projectVersion.set(project.version)
          pactDir.set(project.file("${project.buildDir}/pacts"))
        }

        project.tasks.register('canIDeploy', PactCanIDeployTask) {
          group = GROUP
          description = 'Check if it is safe to deploy by checking whether or not the ' +
                  'specified pacticipant versions are compatible'
          broker.set(extension.broker)
          pacticipant.set(project.hasProperty(PACTICIPANT) ? project.property(PACTICIPANT) : null)
          pacticipantVersion.set(project.hasProperty(PACTICIPANT_VERSION) ? project.property(PACTICIPANT_VERSION)
            : null)
          toProp.set(project.hasProperty(TO) ? project.property(TO) : null)
          latestProp.set(project.hasProperty(LATEST) ? project.property(LATEST) : null)
          toEnvironment.set(project.hasProperty(TO_ENVIRONMENT) ? project.property(TO_ENVIRONMENT) : null)
          toMainBranch.set(project.hasProperty(TO_MAIN_BRANCH) ? project.property(TO_MAIN_BRANCH) : null)
        }

        project.afterEvaluate {
          if (it.pact == null) {
            throw new GradleScriptException('No pact block was found in the project', null)
          } else if (!(it.pact instanceof PactPluginExtension)) {
            throw new GradleScriptException('Your project is misconfigured, was expecting a \'pact\' configuration ' +
              "in the build, but got a ${it.pact.class.simpleName} with value '${it.pact}' instead. " +
              'Make sure there is no property that is overriding \'pact\'.', null)
          }

          it.pact.serviceProviders.all { GradleProviderInfo provider ->
            setupPactConsumersFromBroker(provider, project, it.pact)

                String taskName = {
                  def defaultName = "pactVerify_${provider.name.replaceAll(/\s+/, '_')}".toString()
                  try {
                    def clazz = this.getClass().classLoader.loadClass('org.gradle.util.NameValidator').metaClass
                    def asValidName = clazz.getMetaMethod('asValidName', [String])
                    if (asValidName) {
                      return asValidName.invoke(clazz.newInstance(), [ defaultName ])
                    }
                    // Gradle versions > 4.6 no longer have an instance method
                    return defaultName
                  } catch (ClassNotFoundException e) {
                    // Earlier versions of Gradle don't have NameValidator
                    // Without it, we just don't change the task name
                    return defaultName
                  } catch (NoSuchMethodException e) {
                    // Gradle versions > 4.6 no longer have an instance method
                    return defaultName
                  }
                } ()

                provider.taskNames = project.gradle.startParameter.taskNames

                def providerTask = project.tasks.register(taskName, PactVerificationTask) {
                    it.group = GROUP
                    it.description = "Verify the pacts against ${provider.name}"

                    it.notCompatibleWithConfigurationCache('Configuration Cache is disabled for this task ' +
                      'because of `executeStateChangeTask`')

                    it.providerToVerify = provider

                    it.taskContainer.addAll(project.tasks)
                    List<URI> classPathUrl = []
                    try {
                        classPathUrl = project.sourceSets.test.runtimeClasspath*.toURI()
                    } catch (MissingPropertyException ignored) {
                        // do nothing, the list will be empty
                    }
                    it.testClasspathURL.set(classPathUrl)
                    it.projectVersion.set(project.version)
                    it.report.set(extension.reports)
                    it.buildDir.set(project.buildDir)
                }

                if (project.tasks.findByName(TEST_CLASSES)) {
                    providerTask.configure {
                      dependsOn TEST_CLASSES
                    }
                }

                if (provider.startProviderTask != null) {
                    providerTask.configure {
                      dependsOn(provider.startProviderTask)
                    }
                }

                if (provider.terminateProviderTask != null) {
                    providerTask.configure {
                      finalizedBy(provider.terminateProviderTask)
                    }
                }

                if (provider.dependencyForPactVerify) {
                    it.pactVerify.dependsOn(providerTask)
                }
            }
        }
    }

  @SuppressWarnings('CatchRuntimeException')
  @CompileStatic
  private void setupPactConsumersFromBroker(GradleProviderInfo provider, Project project, PactPluginExtension ext) {
    if (ext.broker && project.gradle.startParameter.taskNames.any {
      it.toLowerCase().contains(PACT_VERIFY.toLowerCase()) }) {
      Map<String, Object> options = [:]
      if (ext.broker.pactBrokerUsername) {
        options.authentication = ['basic', ext.broker.pactBrokerUsername, ext.broker.pactBrokerPassword]
      } else if (ext.broker.pactBrokerToken) {
        options.authentication = ['bearer', ext.broker.pactBrokerToken, ext.broker.pactBrokerAuthenticationHeader]
      }
      if (provider.brokerConfig.enablePending) {
        options.enablePending = true
        options.providerTags = provider.brokerConfig.providerTags
      }
      try {
        provider.consumers = provider.hasPactsFromPactBrokerWithSelectorsV2(options, ext.broker.pactBrokerUrl,
          provider.brokerConfig.selectors)
      } catch (RuntimeException ex) {
        throw new GradleScriptException("Failed to fetch pacts from pact broker ${ext.broker.pactBrokerUrl}",
          ex)
      }
    }
  }
}
