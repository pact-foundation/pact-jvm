package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.messaging.MessagePact

/**
 * Pact runner that only verifies message pacts
 */
open class MessagePactRunner(clazz: Class<*>) : PactRunner(clazz) {
  override fun filterPacts(pacts: List<Pact>): List<Pact> {
    return super.filterPacts(pacts).filter { pact ->
      pact is MessagePact || (pact is FilteredPact && pact.pact is MessagePact)
    }
  }
}
