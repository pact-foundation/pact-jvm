package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.RequestResponsePact

open class RestPactRunner(clazz: Class<*>) : PactRunner(clazz) {
  override fun filterPacts(pacts: List<Pact>): List<Pact> {
    return super.filterPacts(pacts).filter { pact ->
      pact is RequestResponsePact || (pact is FilteredPact && pact.pact is RequestResponsePact)
    }
  }
}
