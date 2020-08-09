package au.com.dius.pact.core.model

/**
 * Pact Specification Version
 */
@Suppress("EnumNaming")
enum class PactSpecVersion {
  UNSPECIFIED, V1, V1_1, V2, V3, V4;

  fun versionString(): String {
    return when (this) {
      V1 -> "1.0.0"
      V1_1 -> "1.1.0"
      V2 -> "2.0.0"
      V3 -> "3.0.0"
      V4 -> "4.0.0"
      else -> "3.0.0"
    }
  }

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
