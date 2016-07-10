package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.ProviderUtils
import au.com.dius.pact.provider.ProviderVerifier
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope

/**
 * Pact Verify Maven Plugin
 */
@Mojo(name = 'verify', requiresDependencyResolution = ResolutionScope.TEST)
class PactProviderMojo extends AbstractMojo {

  @Parameter(defaultValue = '${project.testClasspathElements}', required = true)
  private List<String> classpathElements

  @Parameter
  private List<Provider> serviceProviders

  @Parameter
  @SuppressWarnings('PrivateFieldCouldBeFinal')
  private Map<String, String> configuration = [:]

  @Override
  void execute() throws MojoExecutionException, MojoFailureException {
    Map failures = [:]
    ProviderVerifier verifier = new ProviderVerifier()
    verifier.projectHasProperty = { this.propertyDefined(it) }
    verifier.projectGetProperty =  { this.property(it) }
    verifier.pactLoadFailureMessage = { consumer ->
      "You must specify the pactfile to execute for consumer '${consumer.name}' (use <pactFile> or <pactUrl>)"
    }
    verifier.isBuildSpecificTask = { false }

    verifier.projectClasspath = {
      List<URL> urls = []
      for (element in classpathElements) {
        urls.add(new File(element).toURI().toURL())
      }
      urls as URL[]
    }

    serviceProviders.each { provider ->
      List consumers = []
      consumers.addAll(provider.consumers)
      if (provider.pactFileDirectory != null) {
          consumers.addAll(loadPactFiles(provider, provider.pactFileDirectory))
      }
      if (provider.pactBrokerUrl != null) {
        consumers.addAll(provider.hasPactsFromPactBroker(provider.pactBrokerUrl.toString()))
      }

      provider.setConsumers(consumers)

      failures << verifier.verifyProvider(provider)
    }

    if (failures.size() > 0) {
      verifier.displayFailures(failures)
      throw new MojoFailureException("There were ${failures.size()} pact failures")
    }
  }

  static List loadPactFiles(def provider, File pactFileDir) {
    try {
      return ProviderUtils.loadPactFiles(provider, pactFileDir)
    } catch (e) {
      throw new MojoFailureException(e.message, e)
    }
  }

  private boolean propertyDefined(String key) {
      System.getProperty(key) != null || configuration.containsKey(key)
  }

  private String property(String key) {
      System.getProperty(key, configuration.get(key))
  }

}
