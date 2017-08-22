package au.com.dius.pact.model

import java.util.function.Predicate

class FilteredPact(val pact: Pact, private val interactionPredicate: Predicate<Interaction>) : Pact by pact {
  override val interactions: List<Interaction>
    get() = pact.interactions.filter { interactionPredicate.test(it) }

  fun isNotFiltered() = pact.interactions.all { interactionPredicate.test(it) }
}
