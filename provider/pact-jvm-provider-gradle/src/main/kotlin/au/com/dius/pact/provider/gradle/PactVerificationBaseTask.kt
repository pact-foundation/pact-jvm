package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException

open class PactVerificationBaseTask : DefaultTask() {
  fun runVerification(verifier: ProviderVerifier, providerToVerify: ProviderInfo) {
    val failures = verifier.verifyProviderReturnResult(providerToVerify).filterIsInstance<VerificationResult.Failed>()
    try {
      if (failures.isNotEmpty()) {
        verifier.displayFailures(failures)
        throw GradleScriptException(
          "There were ${failures.sumBy { it.failures.size }} pact failures for provider ${providerToVerify.name}", null)
      }
    } finally {
      verifier.finaliseReports()
    }
  }
}
