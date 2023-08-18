package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.model.v4.V4InteractionType

/**
 * Pact runner that only verifies message pacts
 */
open class MessagePactRunner(clazz: Class<*>) : PactRunner(clazz) {
  override fun filterPacts(pacts: List<Pact>): List<Pact> {
    return super.filterPacts(pacts).filter { pact ->
      isMessagePact(pact) || (pact is FilteredPact && isMessagePact(pact.pact))
    }
  }

  private fun isMessagePact(pact: Pact) = pact is MessagePact ||
    (pact is V4Pact && pact.hasInteractionsOfType(V4InteractionType.AsynchronousMessages))
}
