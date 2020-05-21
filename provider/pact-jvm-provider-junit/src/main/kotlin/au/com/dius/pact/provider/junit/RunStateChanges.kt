package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.ProviderState
import mu.KLogging
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import java.util.function.Supplier
import kotlin.reflect.full.isSubclassOf

data class StateChangeCallbackFailed(
  override val message: String,
  override val cause: Throwable
) : Exception(message, cause)

class RunStateChanges(
  private val next: Statement,
  private val methods: List<Pair<FrameworkMethod, State>>,
  private val stateChangeHandlers: List<Supplier<out Any>>,
  private val providerState: ProviderState,
  private val testContext: MutableMap<String, Any>
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
          throw StateChangeCallbackFailed("State change method for \"${providerState.name}\" failed", e)
        }

        if (stateChangeValue is Map<*, *>) {
          testContext.putAll(stateChangeValue as Map<String, Any>)
        }
      }
    }
  }

  companion object : KLogging()
}
