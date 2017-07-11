package au.com.dius.pact.provider.reporters

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.UrlPactSource
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole

/**
 * Pact verifier reporter that displays the results of the verification to the console using ASCII escapes
 */
@SuppressWarnings(['DuplicateStringLiteral', 'MethodCount'])
class AnsiConsoleReporter implements VerifierReporter {

  boolean displayFullDiff = false

  @Override
  void setReportDir(File reportDir) { }

  @Override
  void setReportFile(File reportFile) { }

  @Override
  void initialise(ProviderInfo provider) { }

  @Override
  void finaliseReport() { }

  @Override
  void reportVerificationForConsumer(ConsumerInfo consumer, ProviderInfo provider) {
    AnsiConsole.out().println(Ansi.ansi().a('\nVerifying a pact between ').bold().a(consumer.name)
      .boldOff().a(' and ').bold().a(provider.name).boldOff())
  }

  @Override
  void verifyConsumerFromUrl(UrlPactSource pactUrl, ConsumerInfo consumer) {
    AnsiConsole.out().println(Ansi.ansi().a("  [from URL $pactUrl.url]"))
  }

  @Override
  void verifyConsumerFromFile(def pactFile, ConsumerInfo consumer) {
    AnsiConsole.out().println(Ansi.ansi().a("  [Using file $pactFile]"))
  }

  @Override
  void pactLoadFailureForConsumer(ConsumerInfo consumerInfo, String message) {
  }

  @Override
  void warnProviderHasNoConsumers(ProviderInfo provider) {
    AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.YELLOW)
      .a("WARNING: There are no consumers to verify for provider '$provider.name'").reset())
  }

  @Override
  void warnPactFileHasNoInteractions(Pact pact) {
    AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.YELLOW)
      .a('WARNING: Pact file has no interactions')
      .reset())
  }

  @Override
  void interactionDescription(Interaction interaction) {
    AnsiConsole.out().println(Ansi.ansi().a('  ').a(interaction.description))
  }

  @Override
  void stateForInteraction(String state, ProviderInfo provider, ConsumerInfo consumer, boolean isSetup) {
    AnsiConsole.out().println(Ansi.ansi().a('  Given ').bold().a(state).boldOff())
  }

  @Override
  void warnStateChangeIgnored(String state, ProviderInfo providerInfo, ConsumerInfo consumerInfo) {
    AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.YELLOW)
      .a('WARNING: State Change ignored as there is no stateChange URL')
      .reset())
  }

  @Override
  @SuppressWarnings(['PrintStackTrace', 'ParameterCount'])
  void stateChangeRequestFailedWithException(String state, ProviderInfo providerInfo, ConsumerInfo consumerInfo,
                                             boolean isSetup, Exception e, boolean printStackTrace) {
    AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.RED).a('State Change Request Failed - ')
      .a(e.message).reset())
    if (printStackTrace) {
      e.printStackTrace()
    }
  }

  @Override
  void stateChangeRequestFailed(String state, ProviderInfo providerInfo, boolean isSetup, String httpStatus) {
    AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.RED)
      .a('State Change Request Failed - ')
      .a(httpStatus).reset())
  }

  @Override
  void warnStateChangeIgnoredDueToInvalidUrl(String state, ProviderInfo providerInfo, boolean isSetup,
                                             def stateChangeHandler) {
    AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.YELLOW)
      .a("WARNING: State Change ignored as there is no stateChange URL, received \"$stateChangeHandler\"")
      .reset())
  }

  @Override
  @SuppressWarnings('PrintStackTrace')
  void requestFailed(ProviderInfo providerInfo, Interaction interaction, String interactionMessage, Exception e,
                     boolean printStackTrace) {
    AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.RED).a('Request Failed - ')
      .a(e.message).reset())
    if (printStackTrace) {
      e.printStackTrace()
    }
  }

  @Override
  void returnsAResponseWhich() {
    AnsiConsole.out().println('    returns a response which')
  }

  @Override
  void statusComparisonOk(int status) {
    AnsiConsole.out().println(Ansi.ansi().a('      ').a('has status code ').bold().a(status).boldOff().a(' (')
      .fg(Ansi.Color.GREEN).a('OK').reset().a(')'))
  }

  @Override
  void statusComparisonFailed(int status, def comparison) {
    AnsiConsole.out().println(Ansi.ansi().a('      ').a('has status code ').bold().a(status).boldOff().a(' (')
      .fg(Ansi.Color.RED).a('FAILED').reset().a(')'))
  }

  @Override
  void includesHeaders() {
    AnsiConsole.out().println('      includes headers')
  }

  @Override
  void headerComparisonOk(String key, String value) {
    AnsiConsole.out().println(Ansi.ansi().a('        "').bold().a(key).boldOff().a('" with value "').bold()
      .a(value).boldOff().a('" (').fg(Ansi.Color.GREEN).a('OK').reset().a(')'))
  }

  @Override
  void headerComparisonFailed(String key, String value, def comparison) {
    AnsiConsole.out().println(Ansi.ansi().a('        "').bold().a(key).boldOff().a('" with value "').bold()
      .a(value).boldOff().a('" (').fg(Ansi.Color.RED).a('FAILED').reset().a(')'))
  }

  @Override
  void bodyComparisonOk() {
    AnsiConsole.out().println(Ansi.ansi().a('      ').a('has a matching body').a(' (')
      .fg(Ansi.Color.GREEN).a('OK').reset().a(')'))
  }

  @Override
  void bodyComparisonFailed(def comparison) {
    AnsiConsole.out().println(Ansi.ansi().a('      ').a('has a matching body').a(' (')
      .fg(Ansi.Color.RED).a('FAILED').reset().a(')'))
  }

  @Override
  void errorHasNoAnnotatedMethodsFoundForInteraction(Interaction interaction) {

  }

  @Override
  @SuppressWarnings('PrintStackTrace')
  void verificationFailed(Interaction interaction, Exception e, boolean printStackTrace) {
    AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.RED).a('Verification Failed - ')
      .a(e.message).reset())
    if (printStackTrace) {
      e.printStackTrace()
    }
  }

  @Override
  void generatesAMessageWhich() {
    AnsiConsole.out().println('    generates a message which')
  }

  @Override
  void displayFailures(Map failures) {
    AnsiConsole.out().println('\nFailures:\n')
    failures.eachWithIndex { err, i ->
      AnsiConsole.out().println("$i) ${err.key}")
      if (err.value instanceof Throwable) {
        displayError(err.value)
      } else if (err.value instanceof Map && err.value.containsKey('comparison') &&
        err.value.comparison instanceof Map) {
        displayDiff(err)
      } else if (err.value instanceof String) {
        AnsiConsole.out().println("      ${err.value}")
      } else if (err.value instanceof Map) {
        err.value.each { key, message ->
          AnsiConsole.out().println("      $key -> $message")
        }
      } else {
        AnsiConsole.out().println("      ${err}")
      }
      AnsiConsole.out().println()
    }
  }

  @SuppressWarnings(['AbcMetric', 'NestedBlockDepth'])
  void displayDiff(err) {
    err.value.comparison.each { key, messageAndDiff ->
      messageAndDiff.each { mismatch ->
        AnsiConsole.out().println("      $key -> ${mismatch.mismatch}")
        AnsiConsole.out().println()

        if (mismatch.diff.any()) {
          AnsiConsole.out().println('        Diff:')
          AnsiConsole.out().println()

          (mismatch.diff instanceof List ? mismatch.diff : [mismatch.diff]).findAll().each {
            it.eachLine { delta ->
              if (delta.startsWith('@')) {
                AnsiConsole.out().println(Ansi.ansi().a('        ').fg(Ansi.Color.CYAN).a(delta).reset())
              } else if (delta.startsWith('-')) {
                AnsiConsole.out().println(Ansi.ansi().a('        ').fg(Ansi.Color.RED).a(delta).reset())
              } else if (delta.startsWith('+')) {
                AnsiConsole.out().println(Ansi.ansi().a('        ').fg(Ansi.Color.GREEN).a(delta).reset())
              } else {
                AnsiConsole.out().println("        $delta")
              }
            }
            AnsiConsole.out().println()
          }
        }
      }
    }

    if (displayFullDiff) {
      AnsiConsole.out().println('      Full Diff:')
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
      AnsiConsole.out().println()
    }
  }

  static void displayError(Throwable err) {
    if (err.message) {
      err.message.split('\n').each {
        AnsiConsole.out().println("      $it")
      }
    } else {
      AnsiConsole.out().println("      ${err.class.name}")
    }
  }
}
