package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.matchers.generators.ArrayContainsJsonGenerator
import au.com.dius.pact.core.matchers.generators.DefaultResponseGenerator
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.InvalidPactException
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessageInteraction
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.V4PactFeaturesException
import au.com.dius.pact.core.support.json.JsonValue
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

@Suppress("TooGenericExceptionCaught", "LongMethod")
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

@Suppress("TooGenericExceptionCaught", "LongMethod")
fun <R> runV4MessageConsumerTest(
  pact: Pact,
  testFunc: MessagePactTestRun<R>
): PactVerificationResult {
  logger.debug { "Running V4 message consumer test with $pact" }
  return try {
    val context = PactTestExecutionContext()
    val messages = pact.interactions.mapNotNull { message ->
      when (message) {
        is V4Interaction.AsynchronousMessage -> {
          val generated = DefaultResponseGenerator.generateContents(
            message.contents, mutableMapOf(
              "ArrayContainsJsonGenerator" to ArrayContainsJsonGenerator
            ), GeneratorTestMode.Consumer, emptyList(), emptyMap(), true
          ) // TODO: need to pass any plugin config here
          V4Interaction.AsynchronousMessage(
            message.key,
            message.description,
            generated,
            message.interactionId,
            message.providerStates,
            message.comments,
            message.pending,
            message.pluginConfiguration,
            message.interactionMarkup,
            message.transport
          )
        }
        is V4Interaction.SynchronousMessages -> {
          val generated = DefaultResponseGenerator.generateContents(
            message.request, mutableMapOf(
              "ArrayContainsJsonGenerator" to ArrayContainsJsonGenerator
            ), GeneratorTestMode.Consumer, emptyList(), emptyMap(), true
          ) // TODO: need to pass any plugin config here

          val generatedResponses = message.response.map {
            DefaultResponseGenerator.generateContents(
              it, mutableMapOf(
                "ArrayContainsJsonGenerator" to ArrayContainsJsonGenerator
              ), GeneratorTestMode.Consumer, emptyList(), emptyMap(), true
            )
          }
          V4Interaction.SynchronousMessages(
            message.key,
            message.description,
            message.interactionId,
            message.providerStates,
            message.comments,
            message.pending,
            generated,
            generatedResponses.toMutableList(),
            message.pluginConfiguration,
            message.interactionMarkup,
            message.transport
          )
        }
        else -> null
      }
    }
    logger.debug { "Calling test function with generated messages: $messages" }
    val result = testFunc.run(messages, context)
    pact.write(context.pactFolder, PactSpecVersion.V4).expect { "Failed to write the Pact" }
    PactVerificationResult.Ok(result)
  } catch (e: Throwable) {
    logger.error(e) { "Consumer test function failed with an exception" }
    PactVerificationResult.Error(e, PactVerificationResult.Ok())
  }
}
