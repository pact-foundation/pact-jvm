package au.com.dius.pact.pactbroker

open class InvalidHalResponse(override val message: String) : RuntimeException(message)

open class NotFoundHalResponse @JvmOverloads constructor(override val message: String = "Not Found") : InvalidHalResponse(message)
