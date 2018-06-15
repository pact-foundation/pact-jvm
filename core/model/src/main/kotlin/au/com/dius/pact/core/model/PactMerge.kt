package au.com.dius.pact.core.model

import mu.KLogging

data class MergeResult<I>(val ok: Boolean, val message: String, val result: Pact<I>? = null) where I: Interaction

/**
 * Utility class for merging two pacts together, checking for conflicts
 */
object PactMerge : KLogging() {

  @JvmStatic
  fun <I> merge(newPact: Pact<I>, existing: Pact<I>): MergeResult<I> where I: Interaction {
    if (!newPact.compatibleTo(existing)) {
      return MergeResult(false, "Cannot merge pacts as they are not compatible")
    }
    if (existing.interactions.isEmpty() || newPact.interactions.isEmpty()) {
      existing.mergeInteractions(newPact.interactions)
      return MergeResult(true, "", existing)
    }

    val conflicts = cartesianProduct(existing.interactions, newPact.interactions)
      .filter { it.first.conflictsWith(it.second) }
    return if (conflicts.isEmpty()) {
      existing.mergeInteractions(newPact.interactions)
      MergeResult(true, "", existing)
    } else {
      MergeResult(false, "Cannot merge pacts as there were ${conflicts.size} conflict(s) " +
        "between the interactions - ${conflicts.joinToString("\n")}")
    }
  }

  private fun cartesianProduct(list1: List<Interaction>, list2: List<Interaction>): List<Pair<Interaction, Interaction>> {
    val result = mutableListOf<Pair<Interaction, Interaction>>()
    list1.forEach { item1 ->
      list2.forEach { item2 -> result.add(item1 to item2) }
    }
    return result
  }
}
