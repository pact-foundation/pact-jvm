package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult

/**
 * Exception to indicate pact failures
 */
class PactFailedException extends RuntimeException {
    private final PactVerificationResult pactVerificationResult

    PactFailedException(PactVerificationResult verificationResult) {
        super(verificationResult.description, verificationResult.metaClass.respondsTo(verificationResult, 'getError')
          ? verificationResult.error : null)
        this.pactVerificationResult = verificationResult
    }
}
