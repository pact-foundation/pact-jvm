package au.com.dius.pact.core.model

import com.github.michaelbull.result.Err
import mu.KLogging

data class MergeResult(val ok: Boolean, val message: String, val result: Pact? = null)

/**
 * Utility class for merging two pacts together, checking for conflicts
 */
object PactMerge : KLogging() {

  @JvmStatic
  fun merge(newPact: Pact, existing: Pact): MergeResult {
    val compatibleTo = newPact.compatibleTo(existing)
    if (compatibleTo is Err) {
      return MergeResult(false, "Cannot merge pacts as they are not compatible - ${compatibleTo.error}")
    }

    return when {
      existing.interactions.isEmpty() -> MergeResult(true, "", newPact)
      newPact.interactions.isEmpty() -> MergeResult(true, "", existing)
      else -> {
        val conflicts = cartesianProduct(existing.interactions, newPact.interactions)
          .filter { it.first.conflictsWith(it.second) }
        if (conflicts.isEmpty()) {
          MergeResult(true, "", existing.mergeInteractions(newPact.interactions))
        } else {
          MergeResult(false, "Cannot merge pacts as there were ${conflicts.size} conflict(s) " +
            "between the interactions - ${conflicts.joinToString("\n")}")
        }
      }
    }
  }

  private fun cartesianProduct(
    list1: List<Interaction>,
    list2: List<Interaction>
  ): List<Pair<Interaction, Interaction>> {
    val result = mutableListOf<Pair<Interaction, Interaction>>()
    list1.forEach { item1 ->
      list2.forEach { item2 -> result.add(item1 to item2) }
    }
    return result
  }
}
