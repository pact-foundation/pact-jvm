package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.support.V4PactFeaturesException
import com.github.michaelbull.result.expect

interface PactTestRun<R> {
  @Throws(Throwable::class)
  fun run(mockServer: MockServer, context: PactTestExecutionContext?): R
}

fun <R> runConsumerTest(pact: Pact, config: MockProviderConfig, test: PactTestRun<R>): PactVerificationResult {
  val errors = pact.validateForVersion(config.pactVersion)
  if (errors.isNotEmpty()) {
    return PactVerificationResult.Error(
      V4PactFeaturesException("Pact specification V4 features can not be used with version " +
        "${config.pactVersion} - ${errors.joinToString(", ")}"), PactVerificationResult.Ok())
  }


  val requestResponsePact = pact.asRequestResponsePact().expect { "Expected an HTTP Request/Response Pact" }
  val server = mockServer(requestResponsePact, config)
  return server.runAndWritePact(requestResponsePact, config.pactVersion, test)
}

interface MessagePactTestRun<R> {
  @Throws(Throwable::class)
  fun run(messages: List<Message>, context: PactTestExecutionContext?): R
}

fun <R> runMessageConsumerTest(
  pact: Pact,
  pactVersion: PactSpecVersion = PactSpecVersion.V3,
  testFunc: MessagePactTestRun<R>
): PactVerificationResult {
  val errors = pact.validateForVersion(pactVersion)
  if (errors.isNotEmpty()) {
    return PactVerificationResult.Error(
      V4PactFeaturesException("Pact specification V4 features can not be used with version " +
        "$pactVersion - ${errors.joinToString(", ")}"), PactVerificationResult.Ok())
  }

  return try {
    val context = PactTestExecutionContext()
    val messagePact = pact.asMessagePact().expect { "Expected a message Pact" }
    val result = testFunc.run(messagePact.messages, context)
    pact.write(context.pactFolder, pactVersion).expect { "Failed to write the Pact" }
    PactVerificationResult.Ok(result)
  } catch (e: Throwable) {
    PactVerificationResult.Error(e, PactVerificationResult.Ok())
  }
}
