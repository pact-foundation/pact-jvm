package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact

interface PactTestRun<R> {
  @Throws(Throwable::class)
  fun run(mockServer: MockServer, context: PactTestExecutionContext?): R
}

fun <R> runConsumerTest(pact: RequestResponsePact, config: MockProviderConfig, test: PactTestRun<R>): PactVerificationResult {
  val server = mockServer(pact, config)
  return server.runAndWritePact(pact, config.pactVersion, test)
}

interface MessagePactTestRun<R> {
  @Throws(Throwable::class)
  fun run(messages: List<Message>, context: PactTestExecutionContext?): R
}

fun <R> runMessageConsumerTest(
  pact: MessagePact,
  pactVersion: PactSpecVersion = PactSpecVersion.V3,
  testFunc: MessagePactTestRun<R>
): PactVerificationResult {
  return try {
    val context = PactTestExecutionContext()
    val result = testFunc.run(pact.messages, context)
    pact.write(context.pactFolder, pactVersion)
    PactVerificationResult.Ok(result)
  } catch (e: Throwable) {
    PactVerificationResult.Error(e, PactVerificationResult.Ok())
  }
}
