package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.MatchingRules
import mu.KLogging

object MetadataMatcher: KLogging() {

  /**
   * Compares the expected metadata value to the actual, delegating to any matching rules if present
   */
  @JvmStatic
  fun compare(key: String, expected: Any?, actual: Any?, matchers: MatchingRules): MetadataMismatch? {
    logger.debug { "Comparing metadata key '$key': '$actual' (${actual?.javaClass?.simpleName}) to '$expected'" +
      " (${expected?.javaClass?.simpleName})" }

    return when {
      Matchers.matcherDefined("metadata", listOf(key), matchers) -> {
        val matchResult = Matchers.domatch(matchers, "metadata", listOf(key), expected, actual,
          MetadataMismatchFactory)
        return matchResult.fold(null as MetadataMismatch?) { acc, item -> acc?.merge(item) ?: item }
      }
      expected == actual -> null
      else -> MetadataMismatch(key, expected, actual, "Expected metadata key '$key' to have value " +
        "'$expected' (${expected?.javaClass?.simpleName}) but was '$actual' (${actual?.javaClass?.simpleName})")
    }
  }

}
