package au.com.dius.pact.server

import au.com.dius.pact.core.support.Result

sealed class VerificationResult {
  object PactVerified: VerificationResult()

  data class PactMismatch(val results: PactSessionResults, val userError: Throwable? = null): VerificationResult() {
    override fun toString(): String {
      var s = "Pact verification failed for the following reasons:\n"
      for (mismatch in results.almostMatched) {
        s += mismatch.description()
      }
      if (results.unexpected.isNotEmpty()) {
        s += "\nThe following unexpected results were received:\n"
        for (unexpectedResult in results.unexpected) {
          s += unexpectedResult.toString()
        }
      }
      if (results.missing.isNotEmpty()) {
        s += "\nThe following requests were not received:\n"
        for (unexpectedResult in results.missing) {
          s += unexpectedResult.toString()
        }
      }
      return s
    }
  }

  data class PactError(val error: Throwable): VerificationResult()

  data class UserCodeFailed<T>(val error: T): VerificationResult()

  // Temporary.  Should belong somewhere else.
  override fun toString(): String = when(this) {
    is PactVerified -> "Pact verified."
    is PactMismatch -> """
      |Missing: ${results.missing.mapNotNull { it.asSynchronousRequestResponse() }.map { it.request }}\n
      |AlmostMatched: ${results.almostMatched}\n
      |Unexpected: ${results.unexpected}\n"""
    is PactError -> "${error.javaClass.getName()} ${error.message}"
    is UserCodeFailed<*> -> "${error?.javaClass?.getName()} $error"
  }

  companion object {
    fun apply(r: Result<PactSessionResults, Exception>): VerificationResult = when (r) {
      is Result.Ok -> {
        if (r.value.allMatched()) PactVerified
        else PactMismatch(r.value)
      }
      is Result.Err -> PactError(r.error)
    }
  }
}
