package au.com.dius.pact.core.matchers

import mu.KLogging

object MetadataMatcher : KLogging() {

  /**
   * Compares the expected metadata value to the actual, delegating to any matching rules if present
   */
  @JvmStatic
  fun compare(key: String, expected: Any?, actual: Any?, context: MatchingContext): MetadataMismatch? {
    logger.debug { "Comparing metadata key '$key': '$actual' (${actual?.javaClass?.simpleName}) to '$expected'" +
      " (${expected?.javaClass?.simpleName})" }

    return when {
      context.matcherDefined(listOf(key)) -> {
        val matchResult = Matchers.domatch(context, listOf(key), expected, actual, MetadataMismatchFactory)
        return matchResult.fold(null as MetadataMismatch?) { acc, item -> acc?.merge(item) ?: item }
      }
      expected == actual -> null
      else -> MetadataMismatch(key, expected, actual, "Expected metadata key '$key' to have value " +
        "'$expected' (${expected?.javaClass?.simpleName}) but was '$actual' (${actual?.javaClass?.simpleName})")
    }
  }
}
