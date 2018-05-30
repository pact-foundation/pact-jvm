package au.com.dius.pact.pactbroker

/**
 * This exception is thrown when we don't receive a HAL response from the broker
 */
open class InvalidHalResponse(override val message: String) : RuntimeException(message)

/**
 * Exception is thrown when we get a 404 response after navigating HAL links
 */
open class NotFoundHalResponse @JvmOverloads constructor(override val message: String = "Not Found") : InvalidHalResponse(message)

/**
  * General request failed exception
  */
open class RequestFailedException(override val message: String) : RuntimeException(message)

/**
 * This exception is raised when an invalid navigation is attempted
 */
open class InvalidNavigationRequest(override val message: String) : RuntimeException(message)
