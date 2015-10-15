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

      provider.setConsumers(consumers)

      failures << verifier.verifyProvider(provider)
    }

    if (failures.size() > 0) {
      verifier.displayFailures(failures)
      throw new MojoFailureException("There were ${failures.size()} pact failures")
    }
  }

  List loadPactFiles(def provider, File pactFileDir) {
    if (!pactFileDir.exists()) {
        throw new MojoFailureException("Pact file directory ($pactFileDir) does not exist")
    }

    if (!pactFileDir.isDirectory()) {
        throw new MojoFailureException("Pact file directory ($pactFileDir) is not a directory")
    }

    if (!pactFileDir.canRead()) {
        throw new MojoFailureException("Pact file directory ($pactFileDir) is not readable")
    }

    AnsiConsole.out().println("Loading pact files for provider ${provider.name} from $pactFileDir")

    List consumers = []
    pactFileDir.eachFileMatch FileType.FILES, ~/.*\.json/, {
        def pactJson = new JsonSlurper().parse(it)
        if (pactJson.provider.name == provider.name) {
            consumers << new Consumer(name: pactJson.consumer.name, pactFile: it)
        } else {
          AnsiConsole.out().println("Skipping ${it} as the provider names don't match provider.name: ${provider.name} vs pactJson.provider.name: ${pactJson.provider.name}")
        }
    }
    AnsiConsole.out().println("Found ${consumers.size()} pact files")
    consumers
  }

  private boolean propertyDefined(String key) {
      System.getProperty(key) != null || configuration.containsKey(key)
  }

  private String property(String key) {
      System.getProperty(key, configuration.get(key))
  }

}
