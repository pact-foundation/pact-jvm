package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Utils.lookupEnvironmentValue
import io.github.oshai.kotlinlogging.KLogging

/**
 * Pact Specification Version
 */
@Suppress("EnumNaming")
enum class PactSpecVersion {
  @Deprecated("Use a null value instead of this value") UNSPECIFIED,
  V1,
  V1_1,
  V2,
  V3,
  V4;

  fun versionString(): String {
    return when (this) {
      V1 -> "1.0.0"
      V1_1 -> "1.1.0"
      V2 -> "2.0.0"
      V3 -> "3.0.0"
      V4 -> "4.0"
      else -> defaultVersion().versionString()
    }
  }

  fun or(other: PactSpecVersion?): PactSpecVersion {
    return if (this == UNSPECIFIED) {
      other?.or(defaultVersion()) ?: defaultVersion()
    } else {
      this
    }
  }

  companion object: KLogging() {
    @JvmStatic
    fun fromInt(version: Int): PactSpecVersion {
      return when (version) {
        1 -> V1
        2 -> V2
        4 -> V4
        else -> V3
      }
    }

    @JvmStatic
    fun defaultVersion(): PactSpecVersion {
      val defaultVer = lookupEnvironmentValue("pact.defaultVersion")
      return if (defaultVer.isNullOrEmpty()) {
        V3
      } else {
        valueOf(defaultVer)
      }
    }
  }
}

fun PactSpecVersion?.atLeast(version: PactSpecVersion): Boolean {
  return if (this == null || this == PactSpecVersion.UNSPECIFIED) {
    PactSpecVersion.defaultVersion().atLeast(version)
  } else {
   this >= version
  }
}

fun PactSpecVersion?.lessThan(version: PactSpecVersion): Boolean {
  return if (this == null || this == PactSpecVersion.UNSPECIFIED) {
    PactSpecVersion.defaultVersion().lessThan(version)
  } else {
    this < version
  }
}
