package au.com.dius.pact.provider.junit

import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.v3.messaging.MessagePact

/**
 * Pact runner that only verifies message pacts
 */
open class MessagePactRunner(clazz: Class<*>) : PactRunner(clazz) {
  override fun filterPacts(pacts: List<Pact>): List<Pact> {
    return pacts.filter { pact -> pact is MessagePact }
  }
}
