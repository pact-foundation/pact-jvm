package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.ProviderVerifier
import groovy.io.FileType
import groovy.json.JsonSlurper
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.fusesource.jansi.AnsiConsole

/**
 * Pact Verify Maven Plugin
 */
@Mojo(name = 'verify')
class PactProviderMojo extends AbstractMojo {

  @Parameter(defaultValue = '${project.runtimeClasspathElements}', required = true, readonly = true)
  private List<String> classpathElements

  @Parameter
  private List<Provider> serviceProviders

  @Parameter
  @SuppressWarnings('PrivateFieldCouldBeFinal')
  private Map<String, String> configuration = [:]

  @Override
  @SuppressWarnings(['ThrowRuntimeException'])
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

      failures << verifier.verifyProvider(provider)
    }

    if (failures.size() > 0) {
      verifier.displayFailures(failures)
      throw new RuntimeException("There were ${failures.size()} pact failures")
    }
  }

  @SuppressWarnings('ThrowRuntimeException')
  List loadPactFiles(def provider, File pactFileDir) {
    if (!pactFileDir.exists()) {
        throw new RuntimeException("Pact file directory ($pactFileDir) does not exist")
    }

    if (!pactFileDir.isDirectory()) {
        throw new RuntimeException("Pact file directory ($pactFileDir) is not a directory")
    }

    if (!pactFileDir.canRead()) {
        throw new RuntimeException("Pact file directory ($pactFileDir) is not readable")
    }

    AnsiConsole.out().println("Loading pact files for provider ${provider.name} from $pactFileDir")

    List consumers = []
    pactFileDir.eachFileMatch FileType.FILES, ~/.*\.json/, {
        def pactJson = new JsonSlurper().parse(it)
        if (pactJson.provider.name == provider.name) {
            consumers << new Consumer(name: pactJson.consumer.name, pactFile: it)
        }
    }
    consumers
  }

  private boolean propertyDefined(String key) {
      System.getProperty(key) != null || configuration.containsKey(key)
  }

  private String property(String key) {
      System.getProperty(key, configuration.get(key))
  }

}
