package au.com.dius.pact.consumer

import au.com.dius.pact.matchers.Mismatch
import au.com.dius.pact.core.model.Request

sealed class PactVerificationResult {
  open fun getDescription() = toString()

  object Ok : PactVerificationResult()

  data class Error(val error: Throwable, val mockServerState: PactVerificationResult) : PactVerificationResult()

  data class PartialMismatch(val mismatches: List<Mismatch>) : PactVerificationResult()

  data class Mismatches(val mismatches: List<PactVerificationResult>) : PactVerificationResult() {
    override fun getDescription(): String {
      return "The following mismatched requests occurred:\n" +
        mismatches.map(PactVerificationResult::getDescription).joinToString("\n")
    }
  }

  data class UnexpectedRequest(val request: Request) : PactVerificationResult() {
    override fun getDescription(): String {
      return "Unexpected Request:\n" + request
    }
  }

  data class ExpectedButNotReceived(val expectedRequests: List<Request>) : PactVerificationResult() {
    override fun getDescription(): String {
      return "The following requests were not received:\n" + expectedRequests.joinToString("\n")
    }
  }
}
