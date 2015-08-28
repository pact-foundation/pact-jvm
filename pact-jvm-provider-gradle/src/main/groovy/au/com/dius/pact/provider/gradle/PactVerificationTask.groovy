package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.model.v3.messaging.MessagePact
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.ResponseComparison
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskAction
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder

import java.lang.reflect.Method

/**
 * Task to verify a pact against a provider
 */
@SuppressWarnings(['DuplicateImport', 'DuplicateStringLiteral'])
class PactVerificationTask extends DefaultTask {

  ProviderInfo providerToVerify

  @TaskAction
  void verifyPact() {
    ProviderVerifier verifier = new ProviderVerifier()
    verifier.projectHasProperty = { project.hasProperty(it) }
    verifier.projectGetProperty =  { project.property(it) }
    verifier.pactLoadFailureMessage = { 'You must specify the pactfile to execute (use pactFile = ...)' }
    verifier.isBuildSpecificTask = { it instanceof Task || it instanceof String && project.tasks.findByName(it) }
    verifier.executeBuildSpecificTask = { t, state ->
      def task = t instanceof String ? project.tasks.getByName(t) : t
      task.setProperty('providerState', state)
      task.ext.providerState = state
      def build = project.task(type: GradleBuild) {
        tasks = [task.name]
      }
      build.execute()
    }

    ext.failures = verifier.verifyProvider(providerToVerify)

    if (ext.failures.size() > 0) {
      displayFailures(ext.failures)
      throw new GradleScriptException(
          "There were ${ext.failures.size()} pact failures for provider ${providerToVerify.name}", null)
    }
  }

  private void displayFailures(failures) {
        AnsiConsole.out().println('\nFailures:\n')
        failures.eachWithIndex { err, i ->
            AnsiConsole.out().println("$i) ${err.key}")
            if (err.value instanceof Throwable) {
                displayError(err.value)
            } else if (err.value instanceof Map && err.value.containsKey('diff')) {
                displayDiff(err)
            } else if (err.value instanceof String) {
                AnsiConsole.out().println("      ${err.value}")
            } else {
                err.value.each { key, message ->
                    AnsiConsole.out().println("      $key -> $message")
                }
            }
            AnsiConsole.out().println()
        }
    }

    void displayDiff(err) {
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
    }

    void displayError(Throwable err) {
        err.message.split('\n').each {
            AnsiConsole.out().println("      $it")
        }
    }

    @SuppressWarnings(['PrintStackTrace', 'ThrowRuntimeException'])
    void verifyResponseByInvokingProviderMethods(def pact, ProviderInfo providerInfo, ConsumerInfo consumer,
                                                 def interaction, String interactionMessage) {
        try {
            def urls = project.sourceSets.test.runtimeClasspath*.toURL() as URL[]
            URLClassLoader loader = new URLClassLoader(urls, GroovyObject.classLoader)
            def configurationBuilder = new ConfigurationBuilder()
                .setScanners(new MethodAnnotationsScanner())
                .addClassLoader(loader)
                .addUrls(loader.URLs)

          def scan = packagesToScan(providerInfo, consumer)
          if (!scan.empty) {
                def filterBuilder = new FilterBuilder()
                scan.each { filterBuilder.include(it) }
                configurationBuilder.filterInputsBy(filterBuilder)
            }

            Reflections reflections = new Reflections(configurationBuilder)
            def methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(PactVerifyProvider)
            def providerMethods = methodsAnnotatedWith.findAll { Method m ->
                logger.debug("Found annotated method $m")
                def annotation = m.annotations.find { it.annotationType().toString() == PactVerifyProvider.toString() }
                logger.debug("Found annotation $annotation")
                annotation?.value() == interaction.description
            }

            if (providerMethods.empty) {
                throw new RuntimeException('No annotated methods were found for interaction ' +
                    "'${interaction.description}'")
            } else {
                if (pact instanceof MessagePact) {
                    verifyMessagePact(providerMethods, interaction as Message, interactionMessage)
                } else {
                    def expectedResponse = interaction.response
                    providerMethods.each {
                        def actualResponse = invokeProviderMethod(it)
                        verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage)
                    }
                }
            }
        } catch (e) {
            AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.RED).a('Verification Failed - ')
                .a(e.message).reset())
            ext.failures[interactionMessage] = e
            if (project.hasProperty('pact.showStacktrace')) {
                e.printStackTrace()
            }
        }
    }

    private List packagesToScan(ProviderInfo providerInfo, ConsumerInfo consumer) {
        consumer.packagesToScan ?: providerInfo.packagesToScan
    }

    void verifyMessagePact(Set methods, Message message, String interactionMessage) {
        methods.each {
            AnsiConsole.out().println('    generates a message which')
            def actualMessage = invokeProviderMethod(it)
            def comparison = ResponseComparison.compareMessage(message, actualMessage)
            def s = ' generates a message which'
            displayBodyResult(ext.failures, comparison, interactionMessage + s)
        }
    }

    def invokeProviderMethod(Method m) {
        m.invoke(m.declaringClass.newInstance())
    }
}
