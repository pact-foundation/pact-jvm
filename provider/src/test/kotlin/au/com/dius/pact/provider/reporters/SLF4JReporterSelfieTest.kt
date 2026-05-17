package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.diffplug.selfie.Selfie.expectSelfie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class SLF4JReporterSelfieTest {

  private lateinit var testAppender: ListAppender<ILoggingEvent>
  private lateinit var slf4jLogger: Logger

  @BeforeEach
  fun setup() {
    testAppender = ListAppender<ILoggingEvent>().also { it.start() }
    slf4jLogger = LoggerFactory.getLogger(SLF4JReporter::class.java) as Logger
    slf4jLogger.addAppender(testAppender)
  }

  @AfterEach
  fun cleanup() {
    slf4jLogger.detachAppender(testAppender)
    testAppender.stop()
  }

  @Test
  fun `snapshots a successful verification log output`() {
    val reporter = ReporterManager.createReporter("slf4j", "/tmp/".toFile())
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

    expectSelfie(capturedLogs()).toMatchDisk()
  }

  @Test
  fun `snapshots warning log output`() {
    val reporter = ReporterManager.createReporter("slf4j", "/tmp/".toFile())
    val provider = ProviderInfo(name = "TestProvider")

    reporter.warnProviderHasNoConsumers(provider)
    reporter.warnPublishResultsSkippedBecauseFiltered()
    reporter.warnPublishResultsSkippedBecauseDisabled("PACT_BROKER_PUBLISH_VERIFICATION_RESULTS")

    expectSelfie(capturedLogs()).toMatchDisk()
  }

  @Test
  fun `snapshots interaction comments with references`() {
    val reporter = ReporterManager.createReporter("slf4j", "/tmp/".toFile())
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

    expectSelfie(capturedLogs()).toMatchDisk()
  }

  private fun capturedLogs() = testAppender.list.joinToString("\n") { "${it.level} ${it.message}" }
}

private fun String.toFile() = java.io.File(this)
