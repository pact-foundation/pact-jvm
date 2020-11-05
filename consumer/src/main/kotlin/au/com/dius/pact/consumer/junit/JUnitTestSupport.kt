package au.com.dius.pact.consumer.junit

import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.consumer.PactMismatchesException
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.PactVerificationResult.Ok
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.messaging.MessagePact

import java.lang.reflect.Method

object JUnitTestSupport {
  /**
   * validates method signature as described at [Pact]
   */
  @JvmStatic
  fun conformsToSignature(m: Method): Boolean {
    val pact = m.getAnnotation(Pact::class.java)
    val conforms = (pact != null &&
      au.com.dius.pact.core.model.Pact::class.java.isAssignableFrom(m.returnType) &&
      m.parameterTypes.size == 1 &&
      m.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.dsl.PactDslWithProvider")))

    if (!conforms && pact != null) {
      throw UnsupportedOperationException("Method ${m.name} does not conform required method signature " +
        "'public au.com.dius.pact.core.model.Pact xxx(PactDslWithProvider builder)'")
    }

    return conforms
  }

  /**
   * validates method signature for a Message Pact test
   */
  @JvmStatic
  fun conformsToMessagePactSignature(m: Method): Boolean {
    val pact = m.getAnnotation(Pact::class.java)
    val hasValidPactSignature = MessagePact::class.java.isAssignableFrom(m.returnType) &&
      m.parameterTypes.size == 1 &&
      m.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.MessagePactBuilder"))

    if (!hasValidPactSignature && pact != null) {
      throw UnsupportedOperationException("Method ${m.name} does not conform required method signature " +
        "'public MessagePact xxx(MessagePactBuilder builder)'")
    }

    return hasValidPactSignature
  }

  @JvmStatic
  fun validateMockServerResult(result: PactVerificationResult) {
    if (result !is Ok) {
      if (result is PactVerificationResult.Error) {
        if (result.mockServerState !is Ok) {
          throw AssertionError("Pact Test function failed with an exception, possibly due to " + result.mockServerState, result.error)
        } else {
          throw AssertionError("Pact Test function failed with an exception: " + result.error.message, result.error)
        }
      } else {
        throw PactMismatchesException(result)
      }
    }
  }
}
