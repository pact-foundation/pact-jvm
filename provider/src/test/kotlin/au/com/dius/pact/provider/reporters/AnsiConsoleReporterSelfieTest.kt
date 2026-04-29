package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import com.diffplug.selfie.Selfie.expectSelfie
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class AnsiConsoleReporterSelfieTest {

  @Test
  fun `snapshots a successful verification output`() {
    val reporter = AnsiConsoleReporter("test", null)
    val provider = ProviderInfo(name = "TestProvider")
    val consumer = ConsumerInfo(name = "TestConsumer")
    val interaction = RequestResponseInteraction("a test interaction", emptyList(), Request(), Response())

    val output = captureOutput {
      reporter.reportVerificationForConsumer(consumer, provider, null)
      reporter.interactionDescription(interaction)
      reporter.returnsAResponseWhich()
      reporter.statusComparisonOk(200)
      reporter.includesHeaders()
      reporter.headerComparisonOk("Content-Type", listOf("application/json"))
      reporter.bodyComparisonOk()
    }

    expectSelfie(stripAnsi(output)).toMatchDisk()
  }

  @Test
  fun `snapshots warning output`() {
    val reporter = AnsiConsoleReporter("test", null)
    val provider = ProviderInfo(name = "TestProvider")

    val output = captureOutput {
      reporter.warnProviderHasNoConsumers(provider)
      reporter.warnPublishResultsSkippedBecauseFiltered()
      reporter.warnPublishResultsSkippedBecauseDisabled("PACT_BROKER_PUBLISH_VERIFICATION_RESULTS")
    }

    expectSelfie(stripAnsi(output)).toMatchDisk()
  }

  @Test
  fun `snapshots interaction comments with references`() {
    val reporter = AnsiConsoleReporter("test", null)
    val provider = ProviderInfo(name = "TestProvider")
    val consumer = ConsumerInfo(name = "TestConsumer")
    val interaction = V4Interaction.SynchronousHttp("key", "a test interaction").also {
      it.addReference("openapi", "operationId", "createUser")
      it.addReference("openapi", "tag", "user")
      it.addReference("jira", "ticket", "PROJ-123")
    }

    val output = captureOutput {
      reporter.reportVerificationForConsumer(consumer, provider, null)
      reporter.interactionDescription(interaction)
      reporter.receive(Event.DisplayInteractionComments(interaction.comments))
      reporter.returnsAResponseWhich()
      reporter.statusComparisonOk(200)
      reporter.bodyComparisonOk()
    }

    expectSelfie(stripAnsi(output)).toMatchDisk()
  }

  private fun captureOutput(action: () -> Unit): String {
    val capture = ByteArrayOutputStream()
    val original = System.out
    System.setOut(PrintStream(capture))
    try {
      action()
    } finally {
      System.setOut(original)
    }
    return capture.toString()
  }

  private fun stripAnsi(text: String) = text.replace(Regex("\\[[0-9;]*m"), "")
}
