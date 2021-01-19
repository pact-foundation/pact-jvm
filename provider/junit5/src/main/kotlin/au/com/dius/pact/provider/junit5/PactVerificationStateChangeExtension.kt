package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.provider.ProviderUtils
import au.com.dius.pact.provider.StateChangeResult
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.junitsupport.IgnoreMissingStateChange
import au.com.dius.pact.provider.junitsupport.MissingStateChangeMethod
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.StateChangeAction
import com.github.michaelbull.result.Err
import mu.KLogging
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import java.lang.reflect.Method

/**
 * JUnit 5 test extension class for executing state change callbacks
 */
class PactVerificationStateChangeExtension(
  private val interaction: Interaction,
  private val pactSource: au.com.dius.pact.core.model.PactSource
) : BeforeTestExecutionCallback, AfterTestExecutionCallback {
  override fun beforeTestExecution(extensionContext: ExtensionContext) {
    logger.debug { "beforeEach for interaction '${interaction.description}'" }
    val store = extensionContext.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val testContext = store.get("interactionContext") as PactVerificationContext

    try {
      val providerStateContext = invokeStateChangeMethods(extensionContext, testContext,
        interaction.providerStates, StateChangeAction.SETUP)
      testContext.executionContext = mutableMapOf("providerState" to providerStateContext)
    } catch (e: Exception) {
      val pending = pactSource is BrokerUrlSource && pactSource.result?.pending == true
      logger.error(e) { "Provider state change callback failed" }
      val error = StateChangeResult(Err(e))
      testContext.testExecutionResult.add(VerificationResult.Failed(
        description = "Provider state change callback failed",
        failures = mapOf(interaction.interactionId.orEmpty() to
          listOf(VerificationFailureType.StateChangeFailure("Provider state change callback failed", error))),
        pending = pending
      ))
      if (!pending) {
        throw AssertionError("Provider state change callback failed", e)
      }
    }
  }

  override fun afterTestExecution(context: ExtensionContext) {
    logger.debug { "afterEach for interaction '${interaction.description}'" }
    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val testContext = store.get("interactionContext") as PactVerificationContext

    try {
      invokeStateChangeMethods(context, testContext, interaction.providerStates, StateChangeAction.TEARDOWN)
    } catch (e: Exception) {
      val pending = pactSource is BrokerUrlSource && pactSource.result?.pending == true
      logger.error(e) { "Provider state change callback failed" }
      val error = StateChangeResult(Err(e))
      testContext.testExecutionResult.add(VerificationResult.Failed(
        description = "Provider state change teardown callback failed",
        failures = mapOf(interaction.interactionId.orEmpty() to listOf(
          VerificationFailureType.StateChangeFailure("Provider state change teardown callback failed", error))),
        pending = pending
      ))
      if (!pending) {
        throw AssertionError("Provider state change callback failed", e)
      }
    }
  }

  private fun invokeStateChangeMethods(
    context: ExtensionContext,
    testContext: PactVerificationContext,
    providerStates: List<ProviderState>,
    action: StateChangeAction
  ): Map<String, Any?> {
    val errors = mutableListOf<String>()

    val providerStateContext = mutableMapOf<String, Any?>()
    providerStates.forEach { state ->
      val stateChangeMethods = findStateChangeMethods(context.requiredTestInstance,
        testContext.stateChangeHandlers, state)
      if (stateChangeMethods.isEmpty()) {
        val message = "Did not find a test class method annotated with @State(\"${state.name}\") \n" +
          "for Interaction \"${testContext.interaction.description}\" \n" +
          "with Consumer \"${testContext.consumer.name}\""
        if (ignoreMissingStateChangeMethod(context.requiredTestClass)) {
          logger.warn { message }
        } else {
          errors.add(message)
        }
      } else {
        stateChangeMethods.filter { it.second.action == action }.forEach { (method, stateAnnotation, instance) ->
          logger.info {
            val name = stateAnnotation.value.joinToString(", ")
            if (stateAnnotation.comment.isNotEmpty()) {
              "Invoking state change method '$name':${stateAnnotation.action} (${stateAnnotation.comment})"
            } else {
              "Invoking state change method '$name':${stateAnnotation.action}"
            }
          }
          val stateChangeValue = if (method.parameterCount > 0) {
            ReflectionSupport.invokeMethod(method, instance, state.params)
          } else {
            ReflectionSupport.invokeMethod(method, instance)
          }

          if (stateChangeValue is Map<*, *>) {
            providerStateContext.putAll(stateChangeValue as Map<String, Any?>)
          }
        }
      }
    }

    if (errors.isNotEmpty()) {
      throw MissingStateChangeMethod(errors.joinToString("\n"))
    }

    return providerStateContext
  }

  private fun findStateChangeMethods(
    testClass: Any,
    stateChangeHandlers: List<Any>,
    state: ProviderState
  ): List<Triple<Method, State, Any>> {
    val stateChangeClasses =
      AnnotationSupport.findAnnotatedMethods(testClass.javaClass, State::class.java, HierarchyTraversalMode.TOP_DOWN)
        .map { it to testClass }
        .plus(stateChangeHandlers.flatMap { handler ->
          AnnotationSupport.findAnnotatedMethods(handler.javaClass, State::class.java, HierarchyTraversalMode.TOP_DOWN)
            .map { it to handler }
        })
    return stateChangeClasses
      .map { Triple(it.first, it.first.getAnnotation(State::class.java), it.second) }
      .filter { it.second.value.any { s -> state.name == s } }
  }

  private fun ignoreMissingStateChangeMethod(testClass: Class<*>): Boolean {
    return ProviderUtils.findAnnotation(testClass, IgnoreMissingStateChange::class.java) != null
  }

  companion object : KLogging()
}
