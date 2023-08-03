package au.com.dius.pact.core.matchers

import io.github.oshai.kotlinlogging.KLogging

object MetadataMatcher : KLogging() {

  /**
   * Compares the expected metadata value to the actual, delegating to any matching rules if present
   */
  @JvmStatic
  fun compare(key: String, expected: Any?, actual: Any?, context: MatchingContext): MetadataMismatch? {
    logger.debug { "Comparing metadata key '$key': '$actual' (${actual?.javaClass?.simpleName}) to '$expected'" +
      " (${expected?.javaClass?.simpleName})" }

    val path = listOf(key)
    return when {
      context.matcherDefined(path) -> {
        val matchResult = Matchers.domatch(context, path, expected, actual, MetadataMismatchFactory)
        return matchResult.fold(null as MetadataMismatch?) { acc, item -> acc?.merge(item) ?: item }
      }
      else -> {
        val matchResult = matchEquality(path, expected, actual, MetadataMismatchFactory)
        return matchResult.fold(null as MetadataMismatch?) { acc, item -> acc?.merge(item) ?: item }
      }
    }
  }
}
