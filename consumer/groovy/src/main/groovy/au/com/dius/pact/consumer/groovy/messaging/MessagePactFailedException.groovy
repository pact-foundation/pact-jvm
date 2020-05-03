package au.com.dius.pact.consumer.groovy.messaging

/**
 * Exception thrown when a message pact consumer test has failed
 */
class MessagePactFailedException extends RuntimeException {
    private final List validationFailures

    MessagePactFailedException(List validationFailures) {
      super("Message pact failed with validation failures: ${validationFailures.join('\n')}")
      this.validationFailures = validationFailures
    }
}
