package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.VerificationResult

class PactFailedException extends RuntimeException {
    private final VerificationResult verificationResult

    def PactFailedException(VerificationResult verificationResult) {
        super(verificationResult.toString(), verificationResult.metaClass.respondsTo(verificationResult, 'error')
            ? verificationResult.error() : null)
        this.verificationResult = verificationResult
    }
}
