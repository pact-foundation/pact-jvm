package au.com.dius.pact.consumer.groovy.messaging

/**
 * Exception thrown when a message pact consumer test has failed
 */
class MessagePactFailedException extends RuntimeException {
    private final List validationFailures

    MessagePactFailedException(List validationFailures) {
        this.validationFailures = validationFailures
    }
}
