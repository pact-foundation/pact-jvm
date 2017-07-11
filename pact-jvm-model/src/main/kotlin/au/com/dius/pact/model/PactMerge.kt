package au.com.dius.pact.model

import com.google.common.collect.Lists
import mu.KLogging

data class MergeResult(val ok: Boolean, val message: String, val result: Pact? = null)

/**
 * Utility class for merging two pacts together, checking for conflicts
 */
object PactMerge : KLogging() {

  @JvmStatic
  fun merge(newPact: Pact, existing: Pact): MergeResult {
    if (!newPact.compatibleTo(existing)) {
      return MergeResult(false, "Cannot merge pacts as they are not compatible")
    }
    if (existing.interactions.isEmpty() || newPact.interactions.isEmpty()) {
      existing.mergeInteractions(newPact.interactions)
      return MergeResult(true, "", existing)
    }

    val conflicts = Lists.cartesianProduct(existing.interactions, newPact.interactions)
      .map { it[0] to it[1] }
      .filter { it.first.conflictsWith(it.second) }
    if (conflicts.isEmpty()) {
      existing.mergeInteractions(newPact.interactions)
      return MergeResult(true, "", existing)
    } else {
      return MergeResult(false, "Cannot merge pacts as there were ${conflicts.size} conflict(s) " +
        "between the interactions - ${conflicts.joinToString("\n")}")
    }
  }
}
