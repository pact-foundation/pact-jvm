package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.matchers.generators.DefaultResponseGenerator
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.support.MetricEvent
import au.com.dius.pact.core.support.Metrics
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderUtils.pluginConfigForInteraction
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.junitsupport.TestDescription
import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.collections.isNotEmpty

/**
 * The instance that holds the context for the test of an interaction. The test target will need to be set on it in
 * the before each phase of the test, and the verifyInteraction method must be called in the test template method.
 */
data class PactVerificationContext @JvmOverloads constructor(
  private val store: ExtensionContext.Store,
  private val context: ExtensionContext,
  var target: TestTarget = HttpTestTarget(port = 8080),
  var verifier: IProviderVerifier? = null,
  var valueResolver: ValueResolver = SystemPropertyResolver,
  var providerInfo: IProviderInfo,
  val consumer: IConsumerInfo,
  val interaction: Interaction,
  val pact: Pact,
  var testExecutionResult: MutableList<VerificationResult.Failed> = mutableListOf(),
  val additionalTargets: MutableList<TestTarget> = mutableListOf()
) {
  val stateChangeHandlers: MutableList<Any> = mutableListOf()
  var executionContext: MutableMap<String, Any>? = null

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
      Metrics.sendMetrics(MetricEvent.ProviderVerificationRan(1, "junit5"))

      val result = validateTestExecution(client, request, testContext.executionContext ?: mutableMapOf(), pact)
      verifier!!.displayOutput(result.flatMap { it.getResultOutput() })

      this.testExecutionResult.addAll(result.filterIsInstance<VerificationResult.Failed>())
      if (testExecutionResult.isNotEmpty()) {
        verifier!!.displayFailures(testExecutionResult)
        if (testExecutionResult.any { !it.pending }) {
          val pactSource = consumer.resolvePactSource()
          val source = if (pactSource is PactSource) {
            pactSource
          } else {
            UnknownPactSource
          }
          val description = TestDescription(interaction, source, null, consumer.toPactConsumer())
          throw AssertionError(description.generateDescription() +
            verifier!!.generateErrorStringFromVerificationResult(testExecutionResult))
        }
      }
    } finally {
      verifier!!.finaliseReports()
    }
  }

  private fun validateTestExecution(
    client: Any?,
    request: Any?,
    context: MutableMap<String, Any>,
    pact: Pact
  ): List<VerificationResult> {
    var interactionMessage = "Verifying a pact between ${consumer.name} and ${providerInfo.name}" +
      " - ${interaction.description}"
    if (interaction.isV4() && interaction.asV4Interaction().pending) {
      interactionMessage += " [PENDING]"
    }

    val targetForInteraction = currentTarget()
    if (targetForInteraction == null) {
      val transport = interaction.asV4Interaction().transport ?: "http"
      val message = "Did not find a test target to execute for the interaction transport '" +
        transport + "'"
      return listOf(
        VerificationResult.Failed(
          message, interactionMessage,
          mapOf(
            interaction.interactionId.orEmpty() to
              listOf(VerificationFailureType.InvalidInteractionFailure(message))
          ),
          consumer.pending
        )
      )
    }

    when (providerInfo.verificationType) {
      null, PactVerification.REQUEST_RESPONSE -> {
        return try {
          val (reqResInteraction, pluginData) = if (interaction is V4Interaction.SynchronousHttp) {
            interaction.asV3Interaction() to interaction.pluginConfiguration.toMap()
          } else {
            interaction as RequestResponseInteraction to emptyMap()
          }
          val pactPluginData = pact.asV4Pact().get()?.pluginData() ?: emptyList()
          val expectedResponse = DefaultResponseGenerator.generateResponse(reqResInteraction.response, context,
            GeneratorTestMode.Provider, pactPluginData, pluginData)
          val actualResponse = targetForInteraction.executeInteraction(client, request)

          listOf(
            verifier!!.verifyRequestResponsePact(
              expectedResponse, actualResponse, interactionMessage, mutableMapOf(),
              reqResInteraction.interactionId.orEmpty(), consumer.pending,
              pluginConfigForInteraction(pact, interaction)
            )
          )
        } catch (e: Exception) {
          verifier!!.reporters.forEach {
            it.requestFailed(
              providerInfo, interaction, interactionMessage, e,
              verifier!!.projectHasProperty.apply(ProviderVerifier.PACT_SHOW_STACKTRACE)
            )
          }
          listOf(
            VerificationResult.Failed(
              "Request to provider failed with an exception", interactionMessage,
              mapOf(
                interaction.interactionId.orEmpty() to
                  listOf(VerificationFailureType.ExceptionFailure("Request to provider failed with an exception",
                    e, interaction))
              ),
              consumer.pending
            )
          )
        }
      }
      PactVerification.PLUGIN -> {
        val v4pact = when(val p = this.pact.asV4Pact()) {
          is Result.Ok -> p.value
          is Result.Err -> return listOf(
            VerificationResult.Failed(
              "Plugins can only be used with V4 Pacts", interactionMessage,
              mapOf(
                interaction.interactionId.orEmpty() to
                  listOf(VerificationFailureType.InvalidInteractionFailure("Plugins can only be used with V4 Pacts"))
              ),
              consumer.pending
            )
          )
        }
        return listOf(verifier!!.verifyInteractionViaPlugin(providerInfo, consumer, v4pact, interaction.asV4Interaction(),
          client, request, context + ("userConfig" to targetForInteraction.userConfig)))
      }
      else -> {
        return listOf(verifier!!.verifyResponseByInvokingProviderMethods(providerInfo, consumer, interaction,
          interaction.description, mutableMapOf(), consumer.pending, pluginConfigForInteraction(pact, interaction)))
      }
    }
  }

  fun withStateChangeHandlers(vararg stateClasses: Any): PactVerificationContext {
    stateChangeHandlers.addAll(stateClasses)
    return this
  }

  fun addStateChangeHandlers(vararg stateClasses: Any) {
    stateChangeHandlers.addAll(stateClasses)
  }

  /**
   * Adds additional targets to the context for the test.
   */
  fun addAdditionalTarget(target: TestTarget) {
    additionalTargets.add(target)
  }

  fun currentTarget(): TestTarget? {
    return if (target.supportsInteraction(interaction)) {
      target
    } else {
      additionalTargets.firstOrNull { it.supportsInteraction(interaction) }
    }
  }
}

fun PactVerificationContext?.hasMultipleTargets() = if (this == null)
  false else additionalTargets.isNotEmpty()
