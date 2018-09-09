package au.com.dius.pact.provider.junit

import au.com.dius.pact.model.ProviderState
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement

class RunStateChanges(
  private val next: Statement,
  private val methods: List<Pair<FrameworkMethod, State>>,
  private val target: Any,
  private val providerState: ProviderState,
  private val testContext: MutableMap<String, Any>
) : Statement() {

  override fun evaluate() {
    invokeStateChangeMethods(StateChangeAction.SETUP)
    next.evaluate()
    invokeStateChangeMethods(StateChangeAction.TEARDOWN)
  }

  private fun invokeStateChangeMethods(action: StateChangeAction) {
    for (method in methods) {
      if (method.second.action == action) {
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
