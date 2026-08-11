package au.com.dius.pact.core.matchers

import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType

/**
 * Configures all the core transport and interaction catalogue entries.
 *
 * There is one interaction entry per interaction type a Pact file can record, and the transport
 * entries are a separate concern - it is only history that the request/response interaction is the
 * original Pact one carried over HTTP.
 */
fun interactionCatalogueEntries(): List<CatalogueEntry> {
  return listOf(
    CatalogueEntry(
      CatalogueEntryType.TRANSPORT, CatalogueEntryProviderType.CORE, "core",
      "http", mapOf()),
    CatalogueEntry(CatalogueEntryType.TRANSPORT, CatalogueEntryProviderType.CORE, "core",
      "https", mapOf()),
    CatalogueEntry(CatalogueEntryType.INTERACTION, CatalogueEntryProviderType.CORE, "core",
      "request-response", mapOf()),
    CatalogueEntry(CatalogueEntryType.INTERACTION, CatalogueEntryProviderType.CORE, "core",
      "message", mapOf()),
    CatalogueEntry(CatalogueEntryType.INTERACTION, CatalogueEntryProviderType.CORE, "core",
      "synchronous-message", mapOf())
  )
}
