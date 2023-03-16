package au.com.dius.pact.core.matchers

import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType

/**
 * Configures all the core transport catalogue entries
 */
fun interactionCatalogueEntries(): List<CatalogueEntry> {
  return listOf(
    CatalogueEntry(
      CatalogueEntryType.TRANSPORT, CatalogueEntryProviderType.CORE, "core",
      "http", mapOf()),
    CatalogueEntry(CatalogueEntryType.TRANSPORT, CatalogueEntryProviderType.CORE, "core",
      "https", mapOf()),
    CatalogueEntry(CatalogueEntryType.INTERACTION, CatalogueEntryProviderType.CORE, "core",
      "message", mapOf()),
    CatalogueEntry(CatalogueEntryType.INTERACTION, CatalogueEntryProviderType.CORE, "core",
      "synchronous-message", mapOf())
  )
}
