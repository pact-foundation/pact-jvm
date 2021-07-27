package au.com.dius.pact.provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier

/**
 * Should always wrap a provider version string with this Supplier in order to avoid repeating any logic.
 */
class ProviderVersion(val source: () -> String?) : Supplier<String> {

  companion object {
    const val FALLBACK_VALUE = "0.0.0"
    const val SNAPSHOT_DEFINITION_STRING = "-SNAPSHOT"
    val snapshotRegex = Regex(".*($SNAPSHOT_DEFINITION_STRING)")
    val logger: Logger = LoggerFactory.getLogger(ProviderVersion::class.java)
  }

  override fun get(): String {
    val version = source().orEmpty().ifEmpty { FALLBACK_VALUE }

    if (version == FALLBACK_VALUE) {
      logger.warn("Provider version not set, defaulting to '$FALLBACK_VALUE'")
    }

    val trimSnapshotProperty = System.getProperty(ProviderVerifier.PACT_PROVIDER_VERSION_TRIM_SNAPSHOT)
    val isTrimSnapshot = if (trimSnapshotProperty.isNullOrBlank()) false else trimSnapshotProperty.toBoolean()
    return if (isTrimSnapshot) trimSnapshot(version) else version
  }

  private fun trimSnapshot(providerVersion: String): String {
    if (providerVersion.contains(SNAPSHOT_DEFINITION_STRING)) {
      return providerVersion.removeRange(snapshotRegex.find(providerVersion)!!.groups[1]!!.range)
    }
    return providerVersion
  }
}
