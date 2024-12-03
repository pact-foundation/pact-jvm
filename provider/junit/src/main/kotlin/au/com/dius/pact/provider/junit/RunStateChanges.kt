package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.StateChangeAction
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import java.util.function.Supplier
import kotlin.reflect.full.isSubclassOf

private val logger = KotlinLogging.logger {}

data class StateChangeCallbackFailed(
  override val message: String,
  override val cause: Throwable
) : Exception(message, cause)

class RunStateChanges(
  private val next: Statement,
  private val methods: List<Pair<FrameworkMethod, State>>,
  private val stateChangeHandlers: List<Supplier<out Any>>,
  private val providerState: ProviderState,
  private val testContext: MutableMap<String, Any>,
  private val verifier: IProviderVerifier
) : Statement() {

  override fun evaluate() {
    invokeStateChangeMethods(StateChangeAction.SETUP)
    try {
      next.evaluate()
    } finally {
      invokeStateChangeMethods(StateChangeAction.TEARDOWN)
    }
  }

  private fun invokeStateChangeMethods(action: StateChangeAction) {
    for (method in methods) {
      if (method.second.action == action) {
        logger.info {
          val name = method.second.value.joinToString(", ")
          if (method.second.comment.isNotEmpty()) {
            "Invoking state change method '$name':${method.second.action} (${method.second.comment})"
          } else {
            "Invoking state change method '$name':${method.second.action}"
          }
        }
        val target = stateChangeHandlers.map(Supplier<out Any>::get).find {
          it::class.isSubclassOf(method.first.declaringClass.kotlin)
        }
        val stateChangeValue = try {
          if (method.first.method.parameterCount == 1) {
            method.first.invokeExplosively(target, providerState.params)
          } else {
            method.first.invokeExplosively(target)
          }
        } catch (e: Throwable) {
          logger.error(e) { "State change method for \"${providerState.name}\" failed" }
          val callbackFailed = StateChangeCallbackFailed("State change method for \"${providerState.name}\" failed", e)
          verifier.reportStateChangeFailed(providerState, callbackFailed, action == StateChangeAction.SETUP)
          verifier.finaliseReports()
          throw callbackFailed
        }

        if (stateChangeValue is Map<*, *>) {
          testContext.putAll(stateChangeValue as Map<String, Any>)
        }
      }
    }
  }
}
