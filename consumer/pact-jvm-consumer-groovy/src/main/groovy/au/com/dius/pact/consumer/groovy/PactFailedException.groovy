package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.VerificationResult

/**
 * Exception to indicate pact failures
 */
class PactFailedException extends RuntimeException {
    private final VerificationResult verificationResult
    private final PactVerificationResult pactVerificationResult

    PactFailedException(VerificationResult verificationResult) {
        super(verificationResult.toString(), verificationResult.metaClass.respondsTo(verificationResult, 'error')
            ? verificationResult.error() : null)
        this.verificationResult = verificationResult
    }

    PactFailedException(PactVerificationResult verificationResult) {
        super(verificationResult.description, verificationResult.metaClass.respondsTo(verificationResult, 'getError')
          ? verificationResult.error : null)
        this.pactVerificationResult = verificationResult
    }
}
