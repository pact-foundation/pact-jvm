package au.com.dius.pact.core.model

/**
 * Exception class to indicate an invalid pact specification
 */
class InvalidPactException(message: String) : RuntimeException(message)

/**
 * Exception class to indicate an invalid path expression used in a matcher or generator
 */
class InvalidPathExpression(message: String) : RuntimeException(message)

/**
 * Exception class to indicate unwrap of a missing body value
 */
class UnwrapMissingBodyException(message: String) : RuntimeException(message)
