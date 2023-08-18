package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.v4.V4InteractionType

open class RestPactRunner(clazz: Class<*>) : PactRunner(clazz) {
  override fun filterPacts(pacts: List<Pact>): List<Pact> {
    return super.filterPacts(pacts).filter { pact ->
      isHttpPact(pact) || (pact is FilteredPact && isHttpPact(pact.pact))
    }
  }

  private fun isHttpPact(pact: Pact) = pact is RequestResponsePact ||
    (pact is V4Pact && pact.hasInteractionsOfType(V4InteractionType.SynchronousHTTP))
}
