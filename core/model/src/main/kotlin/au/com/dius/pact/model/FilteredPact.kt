package au.com.dius.pact.model

import java.util.function.Predicate

class FilteredPact<I>(val pact: Pact<I>, private val interactionPredicate: Predicate<I>) : Pact<I> by pact
  where I: Interaction {
  override val interactions: List<I>
    get() = pact.interactions.filter { interactionPredicate.test(it) }

  fun isNotFiltered() = pact.interactions.all { interactionPredicate.test(it) }

  fun isFiltered() = pact.interactions.any { !interactionPredicate.test(it) }

  override fun toString(): String {
    return "FilteredPact(pact=$pact, filtered=${isFiltered()})"
  }
}
