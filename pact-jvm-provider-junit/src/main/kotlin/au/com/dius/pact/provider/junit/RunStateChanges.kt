package au.com.dius.pact.provider.junit

import au.com.dius.pact.model.ProviderState
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement

class RunStateChanges(
  private val next: Statement,
  private val methods: List<FrameworkMethod>,
  private val target: Any,
  private val providerState: ProviderState,
  private val testContext: MutableMap<String, Any>
) : Statement() {

  override fun evaluate() {
    for (method in methods) {
      val stateChangeValue = if (method.method.parameterCount == 1) {
        method.invokeExplosively(target, providerState.params)
      } else {
        method.invokeExplosively(target)
      }

      if (stateChangeValue is Map<*, *>) {
        testContext.putAll(stateChangeValue as Map<String, Any>)
      }
    }
    next.evaluate()
  }
}
