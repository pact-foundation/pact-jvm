package au.com.dius.pact.consumer

import au.com.dius.pact.model.MockProviderConfig
import au.com.dius.pact.core.model.RequestResponsePact

interface PactTestRun {
  @Throws(Throwable::class)
  fun run(mockServer: MockServer, context: PactTestExecutionContext?)
}

fun runConsumerTest(pact: RequestResponsePact, config: MockProviderConfig, test: PactTestRun): PactVerificationResult {
  val server = mockServer(pact, config)
  return server.runAndWritePact(pact, config.pactVersion, test)
}
