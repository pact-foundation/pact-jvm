package au.com.dius.pact.consumer.groovy.messaging

/**
 * Exception thrown when a message pact consumer test has failed
 */
open class MessagePactFailedException(val validationFailures: List<String>):
  RuntimeException("Message pact failed with validation failures: ${validationFailures.joinToString("\n")}")
