package au.com.dius.pact.consumer

import au.com.dius.pact.model.Request
import au.com.dius.pact.model.RequestPartMismatch

sealed class PactVerificationResult {
  object Ok : PactVerificationResult()
  data class Error(val error: Throwable, val mockServerState: PactVerificationResult) : PactVerificationResult()
  data class PartialMismatch(val mismatches: List<RequestPartMismatch>) : PactVerificationResult()
  data class Mismatches(val mismatches: List<PactVerificationResult>) : PactVerificationResult()
  data class UnexpectedRequest(val request: Request) : PactVerificationResult()
  data class ExpectedButNotReceived(val expectedRequests: List<Request>) : PactVerificationResult()
}
