package au.com.dius.pact.consumer;

public class PactMismatchesException extends AssertionError {
  private final PactVerificationResult mismatches;

  public PactMismatchesException(PactVerificationResult mismatches) {
    super(mismatches.getDescription());
    this.mismatches = mismatches;
  }
}
