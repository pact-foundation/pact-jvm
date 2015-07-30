package au.com.dius.pact.consumer.groovy.messaging

class MessagePactFailedException extends RuntimeException {
    private final List validationFailures

    def MessagePactFailedException(List validationFailures) {
        this.validationFailures = validationFailures
    }
}
