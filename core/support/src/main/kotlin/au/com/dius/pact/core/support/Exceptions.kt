package au.com.dius.pact.core.support

import java.lang.RuntimeException

class V4PactFeaturesException(message: String) : RuntimeException(message)

class InvalidEitherOptionException(error: String) : Exception(error)
