package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.VerificationResult
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException

open class PactVerificationBaseTask : DefaultTask() {
  fun runVerification(verifier: IProviderVerifier, providerToVerify: ProviderInfo) {
    val failures = verifier.verifyProvider(providerToVerify).filterIsInstance<VerificationResult.Failed>()
    try {
      if (failures.isNotEmpty()) {
        verifier.displayFailures(failures)
        val nonPending = failures.filterNot { it.pending }
        if (nonPending.isNotEmpty()) {
          throw GradleScriptException(
            "There were ${nonPending.sumBy { it.failures.size }} non-pending pact failures for provider ${providerToVerify.name}", null)
        }
      }
    } finally {
      verifier.finaliseReports()
    }
  }
}
