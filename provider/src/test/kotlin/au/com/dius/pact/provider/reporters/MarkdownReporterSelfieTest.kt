package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.provider.BodyComparisonResult
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import com.diffplug.selfie.Selfie.expectSelfie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class MarkdownReporterSelfieTest {

  private lateinit var reportDir: File

  @BeforeEach
  fun setup() {
    reportDir = createTempDirectory("selfie-md-").toFile()
  }

  @AfterEach
  fun cleanup() {
    reportDir.deleteRecursively()
  }

  @Test
  fun `snapshots a successful verification report`() {
    val reporter = MarkdownReporter("test", reportDir)
    val provider = ProviderInfo(name = "TestProvider")
    val consumer = ConsumerInfo(name = "TestConsumer")
    val interaction = RequestResponseInteraction("a test interaction", emptyList(), Request(), Response())

    reporter.initialise(provider)
    reporter.reportVerificationForConsumer(consumer, provider, null)
    reporter.interactionDescription(interaction)
    reporter.returnsAResponseWhich()
    reporter.statusComparisonOk(200)
    reporter.includesHeaders()
    reporter.headerComparisonOk("Content-Type", listOf("application/json"))
    reporter.bodyComparisonOk()
    reporter.finaliseReport()

    expectSelfie(normalizeOutput(File(reportDir, "TestProvider.md").readText())).toMatchDisk()
  }

  @Test
  fun `snapshots a failed verification report`() {
    val reporter = MarkdownReporter("test", reportDir)
    val provider = ProviderInfo(name = "TestProvider")
    val consumer = ConsumerInfo(name = "TestConsumer")
    val interaction = RequestResponseInteraction("a test interaction", emptyList(), Request(), Response())

    reporter.initialise(provider)
    reporter.reportVerificationForConsumer(consumer, provider, null)
    reporter.interactionDescription(interaction)
    reporter.returnsAResponseWhich()
    reporter.statusComparisonFailed(200, "expected 201 but was 200")
    reporter.includesHeaders()
    reporter.headerComparisonFailed(
      "Content-Type", listOf("application/json"),
      listOf(HeaderMismatch("Content-Type", "application/json", "text/plain",
        "Expected 'application/json' but received 'text/plain'"))
    )
    reporter.bodyComparisonFailed(
      Result.Ok(BodyComparisonResult(
        mapOf("$.name" to listOf(BodyMismatch(
          JsonParser.parseString("\"expected\""),
          JsonParser.parseString("\"actual\""),
          "Expected \"expected\" but received \"actual\"",
          "$.name",
          "- expected\n+ actual"
        ))),
        listOf("- expected", "+ actual")
      ))
    )
    reporter.finaliseReport()

    expectSelfie(normalizeOutput(File(reportDir, "TestProvider.md").readText())).toMatchDisk()
  }

  @Test
  fun `snapshots a pending consumer verification report`() {
    val reporter = MarkdownReporter("test", reportDir)
    val provider = ProviderInfo(name = "TestProvider")
    val consumer = ConsumerInfo(name = "PendingConsumer", pending = true)
    val interaction = RequestResponseInteraction("a pending interaction", emptyList(), Request(), Response())

    reporter.initialise(provider)
    reporter.reportVerificationForConsumer(consumer, provider, "main")
    reporter.interactionDescription(interaction)
    reporter.finaliseReport()

    expectSelfie(normalizeOutput(File(reportDir, "TestProvider.md").readText())).toMatchDisk()
  }

  @Test
  fun `snapshots interaction comments with references`() {
    val reporter = MarkdownReporter("test", reportDir)
    val provider = ProviderInfo(name = "TestProvider")
    val consumer = ConsumerInfo(name = "TestConsumer")
    val interaction = V4Interaction.SynchronousHttp("key", "a test interaction").also {
      it.addReference("openapi", "operationId", "createUser")
      it.addReference("openapi", "tag", "user")
      it.addReference("jira", "ticket", "PROJ-123")
    }

    reporter.initialise(provider)
    reporter.reportVerificationForConsumer(consumer, provider, null)
    reporter.interactionDescription(interaction)
    reporter.receive(Event.DisplayInteractionComments(interaction.comments))
    reporter.returnsAResponseWhich()
    reporter.statusComparisonOk(200)
    reporter.bodyComparisonOk()
    reporter.finaliseReport()

    expectSelfie(normalizeOutput(File(reportDir, "TestProvider.md").readText())).toMatchDisk()
  }

  private fun normalizeOutput(content: String) = content
    .replace(Regex("""\| Date Generated \| [^|\n]+\|"""), "| Date Generated | <timestamp> |")
    .replace(Regex("""\| Pact Version   \| [^|\n]+\|"""), "| Pact Version   | <version> |")
}
