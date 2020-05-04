package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.provider.ProviderVerifier
import org.apache.http.HttpRequest
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestTemplateInvocationContext

object DummyTestTemplate : TestTemplateInvocationContext, ParameterResolver {

  override fun getDisplayName(invocationIndex: Int) = "No pacts found to verify"

  override fun getAdditionalExtensions(): MutableList<Extension> {
    return mutableListOf(this)
  }

  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
    return when (parameterContext.parameter.type) {
      Pact::class.java -> true
      Interaction::class.java -> true
      HttpRequest::class.java -> true
      PactVerificationContext::class.java -> true
      ProviderVerifier::class.java -> true
      else -> false
    }
  }

  override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
    return null
  }
}
