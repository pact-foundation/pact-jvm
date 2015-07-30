package au.com.dius.pact.consumer.groovy

/**
 * Exception for pact errors
 */
class InvalidPactException extends RuntimeException {

  InvalidPactException(String message) {
    super(message)
  }
}
