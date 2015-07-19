package au.com.dius.pact.consumer.groovy

class InvalidMatcherException extends RuntimeException {

  InvalidMatcherException() {
  }

  InvalidMatcherException(String message) {
    super(message)
  }
}
