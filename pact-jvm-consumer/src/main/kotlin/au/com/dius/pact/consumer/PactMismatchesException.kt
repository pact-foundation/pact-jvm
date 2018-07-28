package au.com.dius.pact.consumer

class PactMismatchesException(private val mismatches: PactVerificationResult) : AssertionError(mismatches.getDescription())
