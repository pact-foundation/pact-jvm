package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.StateChangeAction
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import java.util.function.Supplier
import kotlin.reflect.full.isSubclassOf

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
        val stateChangeValue = if (method.first.method.parameterCount == 1) {
          method.first.invokeExplosively(target, providerState.params)
        } else {
          method.first.invokeExplosively(target)
        }

        if (stateChangeValue is Map<*, *>) {
          testContext.putAll(stateChangeValue as Map<String, Any>)
        }
      }
    }
  }
}
