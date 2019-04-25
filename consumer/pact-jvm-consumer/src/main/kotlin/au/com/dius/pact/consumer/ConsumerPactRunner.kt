package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.RequestResponsePact

interface PactTestRun<R> {
  @Throws(Throwable::class)
  fun run(mockServer: MockServer, context: PactTestExecutionContext?): R
}

fun <R> runConsumerTest(pact: RequestResponsePact, config: MockProviderConfig, test: PactTestRun<R>): PactVerificationResult {
  val server = mockServer(pact, config)
  return server.runAndWritePact(pact, config.pactVersion, test)
}
