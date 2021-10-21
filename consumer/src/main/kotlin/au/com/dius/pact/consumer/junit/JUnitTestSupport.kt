package au.com.dius.pact.consumer.junit

import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.consumer.PactMismatchesException
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.PactVerificationResult.Ok
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.messaging.MessagePact

import java.lang.reflect.Method

object JUnitTestSupport {
  /**
   * validates method signature as described at [Pact]
   */
  @JvmStatic
  fun conformsToSignature(m: Method, pactVersion: PactSpecVersion): Boolean {
    val pact = m.getAnnotation(Pact::class.java)
    val conforms = if (pactVersion >= PactSpecVersion.V4) {
      (pact != null &&
        V4Pact::class.java.isAssignableFrom(m.returnType) &&
        m.parameterTypes.size == 1 &&
        (m.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.dsl.PactDslWithProvider")) ||
          m.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.dsl.PactBuilder"))))
    } else {
      (pact != null &&
        au.com.dius.pact.core.model.RequestResponsePact::class.java.isAssignableFrom(m.returnType) &&
        m.parameterTypes.size == 1 &&
        m.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.dsl.PactDslWithProvider")))
    }

    if (!conforms && pact != null) {
      if (pactVersion == PactSpecVersion.V4) {
        throw UnsupportedOperationException("Method ${m.name} does not conform required method signature " +
          "'public au.com.dius.pact.core.model.V4Pact xxx(PactBuilder builder)'")
      } else {
        throw UnsupportedOperationException("Method ${m.name} does not conform required method signature " +
          "'public au.com.dius.pact.core.model.RequestResponsePact xxx(PactDslWithProvider builder)'")
      }
    }

    return conforms
  }

  /**
   * validates method signature for a Message Pact test
   */
  @JvmStatic
  fun conformsToMessagePactSignature(m: Method, pactVersion: PactSpecVersion): Boolean {
    val pact = m.getAnnotation(Pact::class.java)
    val hasValidPactSignature = if (pactVersion >= PactSpecVersion.V4) {
      V4Pact::class.java.isAssignableFrom(m.returnType) &&
        m.parameterTypes.size == 1 &&
        (m.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.MessagePactBuilder")) ||
          m.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.dsl.PactBuilder")))
    } else {
      MessagePact::class.java.isAssignableFrom(m.returnType) &&
        m.parameterTypes.size == 1 &&
        m.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.MessagePactBuilder"))
    }

    if (!hasValidPactSignature && pact != null) {
      if (pactVersion == PactSpecVersion.V4) {
        throw UnsupportedOperationException("Method ${m.name} does not conform required method signature " +
          "'public V4Pact xxx(PactBuilder builder)'")
      } else {
        throw UnsupportedOperationException("Method ${m.name} does not conform required method signature " +
          "'public MessagePact xxx(MessagePactBuilder builder)'")
      }
    }

    return hasValidPactSignature
  }


  /**
   * validates method signature for a synchronous message Pact test
   */
  @JvmStatic
  fun conformsToSynchMessagePactSignature(m: Method, pactVersion: PactSpecVersion): Boolean {
    val pact = m.getAnnotation(Pact::class.java)
    val hasValidPactSignature = if (pactVersion >= PactSpecVersion.V4) {
      V4Pact::class.java.isAssignableFrom(m.returnType) &&
        m.parameterTypes.size == 1 &&
        (m.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.dsl.PactBuilder")) ||
          m.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.dsl.SynchronousMessagePactBuilder")))
    } else {
      false
    }

    if (!hasValidPactSignature && pact != null) {
      throw UnsupportedOperationException("Method ${m.name} does not conform required method signature " +
        "'public V4Pact xxx(PactBuilder|SynchronousMessagePactBuilder builder)'")
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
