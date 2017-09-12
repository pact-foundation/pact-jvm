package au.com.dius.pact.model

import org.apache.commons.collections4.Predicate

class FilteredPact(val pact: Pact, private val interactionPredicate: Predicate<Interaction>) : Pact by pact {
  override val interactions: List<Interaction>
    get() = pact.interactions.filter { interactionPredicate.evaluate(it) }

  fun isNotFiltered() = pact.interactions.all { interactionPredicate.evaluate(it) }
}
