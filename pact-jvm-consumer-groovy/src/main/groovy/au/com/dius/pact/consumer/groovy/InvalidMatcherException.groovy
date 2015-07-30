package au.com.dius.pact.consumer.groovy

/**
 * Exception for handling invalid matchers
 */
class InvalidMatcherException extends RuntimeException {

  InvalidMatcherException(String message) {
    super(message)
  }
}
