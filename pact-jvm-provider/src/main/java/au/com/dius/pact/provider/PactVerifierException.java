package au.com.dius.pact.provider;

/**
 * Exception indicating failure to setup pact verification
 */
public class PactVerifierException extends RuntimeException {
  public PactVerifierException() {
  }

  public PactVerifierException(String message) {
    super(message);
  }

  public PactVerifierException(String message, Throwable cause) {
    super(message, cause);
  }

  public PactVerifierException(Throwable cause) {
    super(cause);
  }
}
