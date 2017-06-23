package au.com.dius.pact.model

/**
 * Pact Specification Version
 */
@Suppress("NamingConventionViolation")
enum class PactSpecVersion {
  V1, V1_1, V2, V3;

  companion object {
    @JvmStatic
    fun fromInt(version: Int): PactSpecVersion {
      return when (version) {
        1 -> V1
        2 -> V2
        else -> V3
      }
    }
  }
}
