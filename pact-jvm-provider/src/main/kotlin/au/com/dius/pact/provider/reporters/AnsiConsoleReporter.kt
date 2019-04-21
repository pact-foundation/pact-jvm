package au.com.dius.pact.provider.reporters

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.PactSource
import au.com.dius.pact.model.UrlPactSource
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import java.io.File

/**
 * Pact verifier reporter that displays the results of the verification to the console using ASCII escapes
 */
class AnsiConsoleReporter(
  private var displayFullDiff: Boolean = false,
  override val ext: String? = null
) : VerifierReporter {

  override fun includesMetadata() {
    AnsiConsole.out().println("      includes message metadata")
  }

  override fun metadataComparisonOk() {
    AnsiConsole.out().println(Ansi.ansi().a("      has matching metadata (")
      .fg(Ansi.Color.GREEN).a("OK").reset().a(")"))
  }

  override fun metadataComparisonOk(key: String, value: Any?) {
    AnsiConsole.out().println(Ansi.ansi().a("        \"").bold().a(key).boldOff().a("\" with value \"")
      .bold().a(value).boldOff().a("\" (").fg(Ansi.Color.GREEN).a("OK").reset().a(")"))
  }

  override fun metadataComparisonFailed(key: String, value: Any?, comparison: Any) {
    AnsiConsole.out().println(Ansi.ansi().a("        \"").bold().a(key).boldOff().a("\" with value \"")
      .bold().a(value).boldOff().a("\" (").fg(Ansi.Color.RED).a("FAILED").reset().a(")"))
  }

  override fun setReportDir(reportDir: File) { }

  override fun setReportFile(reportFile: File) { }

  override fun initialise(provider: IProviderInfo) { }

  override fun finaliseReport() { }

  override fun reportVerificationForConsumer(consumer: IConsumerInfo, provider: IProviderInfo) {
    AnsiConsole.out().println(Ansi.ansi().a("\nVerifying a pact between ").bold().a(consumer.name)
      .boldOff().a(" and ").bold().a(provider.name).boldOff())
  }

  override fun verifyConsumerFromUrl(pactUrl: UrlPactSource, consumer: IConsumerInfo) {
    AnsiConsole.out().println(Ansi.ansi().a("  [from ${pactUrl.description()}]"))
  }

  override fun verifyConsumerFromFile(pactFile: PactSource, consumer: IConsumerInfo) {
    AnsiConsole.out().println(Ansi.ansi().a("  [Using ${pactFile.description()}]"))
  }

  override fun pactLoadFailureForConsumer(consumer: IConsumerInfo, message: String) { }

  override fun warnProviderHasNoConsumers(provider: IProviderInfo) {
    AnsiConsole.out().println(Ansi.ansi().a("         ").fg(Ansi.Color.YELLOW)
      .a("WARNING: There are no consumers to verify for provider '${provider.name}'").reset())
  }

  override fun warnPactFileHasNoInteractions(pact: Pact<Interaction>) {
    AnsiConsole.out().println(Ansi.ansi().a("         ").fg(Ansi.Color.YELLOW)
      .a("WARNING: Pact file has no interactions")
      .reset())
  }

  override fun interactionDescription(interaction: Interaction) {
    AnsiConsole.out().println(Ansi.ansi().a("  ").a(interaction.description))
  }

  override fun stateForInteraction(state: String, provider: IProviderInfo, consumer: IConsumerInfo, isSetup: Boolean) {
    AnsiConsole.out().println(Ansi.ansi().a("  Given ").bold().a(state).boldOff())
  }

  override fun warnStateChangeIgnored(state: String, IProviderInfo: IProviderInfo, IConsumerInfo: IConsumerInfo) {
    AnsiConsole.out().println(Ansi.ansi().a("         ").fg(Ansi.Color.YELLOW)
      .a("WARNING: State Change ignored as there is no stateChange URL")
      .reset())
  }

  override fun stateChangeRequestFailedWithException(
    state: String,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    isSetup: Boolean,
    e: Exception,
    printStackTrace: Boolean
  ) {
    AnsiConsole.out().println(Ansi.ansi().a("         ")
      .fg(Ansi.Color.RED).a("State Change Request Failed - ")
      .a(e.message).reset())
    if (printStackTrace) {
      e.printStackTrace()
    }
  }

  override fun stateChangeRequestFailed(state: String, provider: IProviderInfo, isSetup: Boolean, httpStatus: String) {
    AnsiConsole.out().println(Ansi.ansi().a("         ").fg(Ansi.Color.RED)
      .a("State Change Request Failed - ")
      .a(httpStatus).reset())
  }

  override fun warnStateChangeIgnoredDueToInvalidUrl(
    state: String,
    provider: IProviderInfo,
    isSetup: Boolean,
    stateChangeHandler: Any
  ) {
    AnsiConsole.out().println(Ansi.ansi().a("         ").fg(Ansi.Color.YELLOW)
      .a("WARNING: State Change ignored as there is no stateChange URL, received \"$stateChangeHandler\"")
      .reset())
  }

  override fun requestFailed(
    provider: IProviderInfo,
    interaction: Interaction,
    interactionMessage: String,
    e: Exception,
    printStackTrace: Boolean
  ) {
    AnsiConsole.out().println(Ansi.ansi().a("      ").fg(Ansi.Color.RED).a("Request Failed - ")
      .a(e.message).reset())
    if (printStackTrace) {
      e.printStackTrace()
    }
  }

  override fun returnsAResponseWhich() {
    AnsiConsole.out().println("    returns a response which")
  }

  override fun statusComparisonOk(status: Int) {
    AnsiConsole.out().println(Ansi.ansi().a("      ").a("has status code ").bold().a(status).boldOff()
      .a(" (").fg(Ansi.Color.GREEN).a("OK").reset().a(")"))
  }

  override fun statusComparisonFailed(status: Int, comparison: Any) {
    AnsiConsole.out().println(Ansi.ansi().a("      ").a("has status code ").bold().a(status).boldOff()
      .a(" (")
      .fg(Ansi.Color.RED).a("FAILED").reset().a(")"))
  }

  override fun includesHeaders() {
    AnsiConsole.out().println("      includes headers")
  }

  override fun headerComparisonOk(key: String, value: List<String>) {
    AnsiConsole.out().println(Ansi.ansi().a("        \"").bold().a(key).boldOff().a("\" with value \"")
      .bold()
      .a(value.joinToString(", ")).boldOff().a("\" (").fg(Ansi.Color.GREEN).a("OK").reset().a(")"))
  }

  override fun headerComparisonFailed(key: String, value: List<String>, comparison: Any) {
    AnsiConsole.out().println(Ansi.ansi().a("        \"").bold().a(key).boldOff().a("\" with value \"")
      .bold()
      .a(value.joinToString(", ")).boldOff().a("\" (").fg(Ansi.Color.RED).a("FAILED").reset().a(")"))
  }

  override fun bodyComparisonOk() {
    AnsiConsole.out().println(Ansi.ansi().a("      ").a("has a matching body").a(" (")
      .fg(Ansi.Color.GREEN).a("OK").reset().a(")"))
  }

  override fun bodyComparisonFailed(comparison: Any) {
    AnsiConsole.out().println(Ansi.ansi().a("      ").a("has a matching body").a(" (")
      .fg(Ansi.Color.RED).a("FAILED").reset().a(")"))
  }

  override fun errorHasNoAnnotatedMethodsFoundForInteraction(interaction: Interaction) { }

  override fun verificationFailed(interaction: Interaction, e: Exception, printStackTrace: Boolean) {
    AnsiConsole.out().println(Ansi.ansi().a("      ").fg(Ansi.Color.RED).a("Verification Failed - ")
      .a(e.message).reset())
    if (printStackTrace) {
      e.printStackTrace()
    }
  }

  override fun generatesAMessageWhich() {
    AnsiConsole.out().println("    generates a message which")
  }

  override fun displayFailures(failures: Map<String, Any>) {
    AnsiConsole.out().println("\nFailures:\n")
    failures.entries.forEachIndexed { i, err ->
      AnsiConsole.out().println("$i) ${err.key}")
      when {
        err.value is Throwable -> displayError(err.value as Throwable)
        err.value is Map<*, *> && (err.value as Map<*, *>).containsKey("comparison") &&
          (err.value as Map<*, *>)["comparison"] is Map<*, *> -> displayDiff(err.value as Map<String, Any>)
        err.value is String -> AnsiConsole.out().println("      ${err.value}")
        err.value is Map<*, *> -> (err.value as Map<*, *>).forEach { key, message ->
          AnsiConsole.out().println("      $key -> $message")
        }
        else -> AnsiConsole.out().println("      $err")
      }
      AnsiConsole.out().println()
    }
  }

  private fun displayDiff(diff: Map<String, Any>) {
    (diff["comparison"] as Map<String, List<Map<String, Any>>>).forEach { key, messageAndDiff ->
      messageAndDiff.forEach { mismatch ->
        AnsiConsole.out().println("      $key -> ${mismatch["mismatch"]}")
        AnsiConsole.out().println()

        val mismatchDiff = if (mismatch["diff"] is List<*>) mismatch["diff"] as List<String>
          else listOf(mismatch["diff"].toString())
        if (mismatchDiff.any { it.isNotEmpty() }) {
          AnsiConsole.out().println("        Diff:")
          AnsiConsole.out().println()

          mismatchDiff.filter { it.isNotEmpty() }.forEach {
            it.split('\n').forEach { delta ->
              when {
                delta.startsWith('@') -> AnsiConsole.out().println(Ansi.ansi().a("        ")
                  .fg(Ansi.Color.CYAN).a(delta).reset())
                delta.startsWith('-') -> AnsiConsole.out().println(Ansi.ansi().a("        ")
                  .fg(Ansi.Color.RED).a(delta).reset())
                delta.startsWith('+') -> AnsiConsole.out().println(Ansi.ansi().a("        ")
                  .fg(Ansi.Color.GREEN).a(delta).reset())
                else -> AnsiConsole.out().println("        $delta")
              }
            }
            AnsiConsole.out().println()
          }
        }
      }
    }

    if (displayFullDiff) {
      AnsiConsole.out().println("      Full Diff:")
      AnsiConsole.out().println()

      (diff["diff"] as List<String>).forEach { delta ->
        when {
          delta.startsWith('@') -> AnsiConsole.out().println(Ansi.ansi().a("      ")
            .fg(Ansi.Color.CYAN).a(delta).reset())
          delta.startsWith('-') -> AnsiConsole.out().println(Ansi.ansi().a("      ")
            .fg(Ansi.Color.RED).a(delta).reset())
          delta.startsWith('+') -> AnsiConsole.out().println(Ansi.ansi().a("      ")
            .fg(Ansi.Color.GREEN).a(delta).reset())
          else -> AnsiConsole.out().println("      $delta")
        }
      }
      AnsiConsole.out().println()
    }
  }

  private fun displayError(err: Throwable) {
    if (!err.message.isNullOrEmpty()) {
      err.message!!.split('\n').forEach {
        AnsiConsole.out().println("      $it")
      }
    } else {
      AnsiConsole.out().println("      ${err.javaClass.name}")
    }
  }
}
