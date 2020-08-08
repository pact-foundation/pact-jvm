package au.com.dius.pact.core.model

/**
 * Pact Specification Version
 */
@Suppress("EnumNaming")
enum class PactSpecVersion {
  UNSPECIFIED, V1, V1_1, V2, V3, V4;

  companion object {
    @JvmStatic
    fun fromInt(version: Int): PactSpecVersion {
      return when (version) {
        1 -> V1
        2 -> V2
        4 -> V4
        else -> V3
      }
    }
  }
}
