package au.com.dius.pact.core.model

import java.util.function.Predicate

class FilteredPact(val pact: Pact, private val interactionPredicate: Predicate<Interaction>) : Pact by pact {
  override val interactions: List<Interaction>
    get() = pact.interactions.filter { interactionPredicate.test(it) }

  fun isNotFiltered() = pact.interactions.all { interactionPredicate.test(it) }

  fun isFiltered() = pact.interactions.any { !interactionPredicate.test(it) }

  override fun toString(): String {
    return "FilteredPact(pact=$pact, filtered=${isFiltered()})"
  }
}
