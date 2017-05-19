package au.com.dius.pact.model

/**
 * Exception class to indicate an invalid pact specification
 */
class InvalidPactException(message: String): RuntimeException(message)

/**
 * Exception class to indicate an invalid path expression used in a matcher or generator
 */
class InvalidPathExpression(message: String) : RuntimeException(message)
