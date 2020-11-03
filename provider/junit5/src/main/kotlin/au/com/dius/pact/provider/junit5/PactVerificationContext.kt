package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * The instance that holds the context for the test of an interaction. The test target will need to be set on it in
 * the before each phase of the test, and the verifyInteraction method must be called in the test template method.
 */
data class PactVerificationContext @JvmOverloads constructor(
  private val store: ExtensionContext.Store,
  private val context: ExtensionContext,
  var target: TestTarget = HttpTestTarget(port = 8080),
  var verifier: IProviderVerifier? = null,
  var valueResolver: ValueResolver = SystemPropertyResolver(),
  var providerInfo: IProviderInfo,
  val consumer: IConsumerInfo,
  val interaction: Interaction,
  var testExecutionResult: MutableList<VerificationResult.Failed> = mutableListOf()
) {
  val stateChangeHandlers: MutableList<Any> = mutableListOf()
  var executionContext: Map<String, Any>? = null

  /**
   * Called to verify the interaction from the test template method.
   *
   * @throws AssertionError Throws an assertion error if the verification fails.
   */
  fun verifyInteraction() {
    val store = context.getStore(namespace)
    val client = store.get("client")
    val request = store.get("request")
    val testContext = store.get("interactionContext") as PactVerificationContext
    try {
      val result = validateTestExecution(client, request, testContext.executionContext ?: emptyMap())
        .filterIsInstance<VerificationResult.Failed>()
      this.testExecutionResult.addAll(result)
      if (testExecutionResult.isNotEmpty()) {
        verifier!!.displayFailures(testExecutionResult)
        if (testExecutionResult.any { !it.pending }) {
          throw AssertionError(verifier!!.generateErrorStringFromVerificationResult(testExecutionResult))
        }
      }
    } finally {
      verifier!!.finaliseReports()
    }
  }

  private fun validateTestExecution(
    client: Any?,
    request: Any?,
    context: Map<String, Any>
  ): List<VerificationResult> {
    if (providerInfo.verificationType == null || providerInfo.verificationType == PactVerification.REQUEST_RESPONSE) {
      val interactionMessage = "Verifying a pact between ${consumer.name} and ${providerInfo.name}" +
        " - ${interaction.description}"
      return try {
        val reqResInteraction = interaction as RequestResponseInteraction
        val expectedResponse = reqResInteraction.response.generatedResponse(context)
        val actualResponse = target.executeInteraction(client, request)

        listOf(verifier!!.verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, mutableMapOf(),
          reqResInteraction.interactionId.orEmpty(), consumer.pending))
      } catch (e: Exception) {
        verifier!!.reporters.forEach {
          it.requestFailed(providerInfo, interaction, interactionMessage, e,
            verifier!!.projectHasProperty.apply(ProviderVerifier.PACT_SHOW_STACKTRACE))
        }
        listOf(VerificationResult.Failed(listOf(mapOf("message" to "Request to provider failed with an exception",
          "exception" to e)),
          "Request to provider failed with an exception", interactionMessage,
          listOf(VerificationFailureType.ExceptionFailure("Request to provider failed with an exception", e)),
          consumer.pending, interaction.interactionId))
      }
    } else {
      return listOf(verifier!!.verifyResponseByInvokingProviderMethods(providerInfo, consumer, interaction,
        interaction.description, mutableMapOf()))
    }
  }

  fun withStateChangeHandlers(vararg stateClasses: Any): PactVerificationContext {
    stateChangeHandlers.addAll(stateClasses)
    return this
  }

  fun addStateChangeHandlers(vararg stateClasses: Any) {
    stateChangeHandlers.addAll(stateClasses)
  }
}
