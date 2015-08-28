package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.ProviderVerifier
import groovy.io.FileType
import groovy.json.JsonSlurper
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
@SuppressWarnings('UnusedImport')
import scala.collection.JavaConverters$

/**
 * Pact Verify Maven Plugin
 */
@Mojo(name = 'verify')
class PactProviderMojo extends AbstractMojo {

  @Parameter
  private List<Provider> serviceProviders

  @Parameter
  @SuppressWarnings('PrivateFieldCouldBeFinal')
  private Map<String, String> configuration = [:]

  @Override
  @SuppressWarnings(['AbcMetric', 'ThrowRuntimeException', 'NestedBlockDepth', 'PrintStackTrace',
      'DuplicateStringLiteral', 'MethodSize', 'Println'])
  void execute() throws MojoExecutionException, MojoFailureException {
    Map failures = [:]
    ProviderVerifier verifier = new ProviderVerifier()
    verifier.projectHasProperty = { this.propertyDefined(it) }
    verifier.projectGetProperty =  { this.property(it) }
    verifier.pactLoadFailureMessage = { consumer ->
      "You must specify the pactfile to execute for consumer '${consumer.name}' (use <pactFile> or <pactUrl>)"
    }
    verifier.isBuildSpecificTask = { false }

    serviceProviders.each { provider ->
      List consumers = []
      consumers.addAll(provider.consumers)
      if (provider.pactFileDirectory != null) {
          consumers.addAll(loadPactFiles(provider, provider.pactFileDirectory))
      }

      failures << verifier.verifyProvider(provider)
    }

    if (failures.size() > 0) {
        AnsiConsole.out().println('\nFailures:\n')
        failures.eachWithIndex { err, i ->
            AnsiConsole.out().println("$i) ${err.key}")
            if (err.value instanceof Exception || err.value instanceof Error) {
                err.value.message.split('\n').each {
                    AnsiConsole.out().println("      $it")
                }
            } else if (err.value instanceof Map && err.value.containsKey('diff')) {
                err.value.comparison.each { key, message ->
                    AnsiConsole.out().println("      $key -> $message")
                }

                AnsiConsole.out().println()
                AnsiConsole.out().println('      Diff:')
                AnsiConsole.out().println()

                err.value.diff.each { delta ->
                    if (delta.startsWith('@')) {
                        AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.CYAN).a(delta).reset())
                    } else if (delta.startsWith('-')) {
                        AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.RED).a(delta).reset())
                    } else if (delta.startsWith('+')) {
                        AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.GREEN).a(delta).reset())
                    } else {
                        AnsiConsole.out().println("      $delta")
                    }
                }
            } else {
                err.value.each { key, message ->
                    AnsiConsole.out().println("      $key -> $message")
                }
            }
            AnsiConsole.out().println()
        }

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
