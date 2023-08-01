package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.matchers.generators.ArrayContainsJsonGenerator
import au.com.dius.pact.core.matchers.generators.DefaultResponseGenerator
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.InvalidPactException
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.support.V4PactFeaturesException
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

interface PactTestRun<R> {
  @Throws(Throwable::class)
  fun run(mockServer: MockServer, context: PactTestExecutionContext?): R
}

fun <R> runConsumerTest(pact: BasePact, config: MockProviderConfig, test: PactTestRun<R>): PactVerificationResult {
  val errors = pact.validateForVersion(config.pactVersion)
  if (errors.isNotEmpty()) {
    return PactVerificationResult.Error(
      V4PactFeaturesException("Pact specification V4 features can not be used with version " +
        "${config.pactVersion} - ${errors.joinToString(", ")}"), PactVerificationResult.Ok())
  }

  if (!pact.isRequestResponsePact()) {
    throw InvalidPactException("Expected an HTTP Request/Response Pact")
  }
  val server = mockServer(pact, config)
  return server.runAndWritePact(pact, config.pactVersion, test)
}

interface MessagePactTestRun<R> {
  @Throws(Throwable::class)
  fun run(messages: List<Interaction>, context: PactTestExecutionContext?): R
}

fun <R> runMessageConsumerTest(
  pact: Pact,
  pactVersion: PactSpecVersion = PactSpecVersion.V3,
  testFunc: MessagePactTestRun<R>
): PactVerificationResult {
  logger.debug { "Running message consumer test with $pact" }
  val errors = pact.validateForVersion(pactVersion)
  if (errors.isNotEmpty()) {
    return PactVerificationResult.Error(
      V4PactFeaturesException("Pact specification V4 features can not be used with version " +
        "$pactVersion - ${errors.joinToString(", ")}"), PactVerificationResult.Ok())
  }

  return try {
    val context = PactTestExecutionContext()
    val messagePact = pact.asMessagePact().expect { "Expected a message Pact" }
    val messages = messagePact.messages.map {
      val generated = DefaultResponseGenerator.generateContents(it.asAsynchronousMessage()!!.contents, mutableMapOf(
        "ArrayContainsJsonGenerator" to ArrayContainsJsonGenerator
      ), GeneratorTestMode.Consumer, emptyList(), emptyMap(), true) // TODO: need to pass any plugin config here
      Message(it.description, it.providerStates, generated.contents, it.matchingRules, it.generators,
        (it.metadata + generated.metadata).toMutableMap(), it.interactionId)
    }
    logger.debug { "Calling test function with generated messages: $messages" }
    val result = testFunc.run(messages, context)
    pact.write(context.pactFolder, pactVersion).expect { "Failed to write the Pact" }
    PactVerificationResult.Ok(result)
  } catch (e: Throwable) {
    logger.error(e) { "Consumer test function failed with an exception" }
    PactVerificationResult.Error(e, PactVerificationResult.Ok())
  }
}
