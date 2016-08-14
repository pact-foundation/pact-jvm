package au.com.dius.pact.model

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * Utility class for merging two pacts together, checking for conflicts
 */
@Singleton
@CompileStatic
class PactMerge {

  @Canonical
  static class MergeResult {
    boolean ok
    String message
    Pact result
  }

  static MergeResult merge(Pact newPact, Pact existing) {
    if (!newPact.compatibleTo(existing)) {
      return new MergeResult(false, 'Cannot merge pacts as they are not compatible')
    }
    if (existing.interactions.empty || newPact.interactions.empty) {
      existing.interactions.addAll(newPact.interactions)
      existing.sortInteractions()
      return new MergeResult(true, '', existing)
    }

    def conflicts = [existing.interactions, newPact.interactions].combinations()
      .findResults {
        List pair = it as List
        Interaction i0 = pair[0] as Interaction
        Interaction i1 = pair[1] as Interaction
        i0.conflictsWith(i1) ? pair : null
      }
    if (conflicts.empty) {
      existing.mergeInteractions(newPact.interactions)
      new MergeResult(true, '', existing)
    } else {
      new MergeResult(false, "Cannot merge pacts as there were ${conflicts.size()} conflicts " +
        'between the interactions')
    }
  }
}
