package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.ProviderUtils
import au.com.dius.pact.provider.ProviderVerifier
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.fusesource.jansi.AnsiConsole

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
    AnsiConsole.systemInstall()

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
      if (provider.pactBrokerUrl || provider.pactBroker) {
        loadPactsFromPactBroker(provider, consumers)
      }

      provider.setConsumers(consumers)

      failures << verifier.verifyProvider(provider)
    }

    if (failures.size() > 0) {
      verifier.displayFailures(failures)
      AnsiConsole.systemUninstall()
      throw new MojoFailureException("There were ${failures.size()} pact failures")
    }
  }

  static void loadPactsFromPactBroker(Provider provider, List consumers) {
    URL pactBrokerUrl = provider.pactBroker?.url ?: provider.pactBrokerUrl
    def options = [:]
    if (provider.pactBroker?.authentication) {
      options.authentication = [
        'basic', provider.pactBroker?.authentication.username, provider.pactBroker?.authentication.password
      ]
    }
    if (provider.pactBroker?.tags) {
      provider.pactBroker.tags.each { String tag ->
        consumers.addAll(provider.hasPactsFromPactBrokerWithTag(options, pactBrokerUrl.toString(), tag))
      }
    } else {
      consumers.addAll(provider.hasPactsFromPactBroker(options, pactBrokerUrl.toString()))
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
