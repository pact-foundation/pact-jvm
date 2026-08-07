package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.plugins.PluginSupportRegistry
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType
import io.pact.plugins.jvm.core.CatalogueManager
import io.pact.plugins.jvm.core.CoreCapabilityRegistry
import io.pact.plugins.jvm.core.CoreContentGenerator
import kotlin.reflect.full.createInstance

object MatchingConfig {
  private val coreBodyMatchers = mapOf(
    "application/vnd.schemaregistry.v1\\+json" to "au.com.dius.pact.core.matchers.KafkaJsonSchemaContentMatcher",
    "application/.*xml" to "au.com.dius.pact.core.matchers.XmlContentMatcher",
    "text/xml" to "au.com.dius.pact.core.matchers.XmlContentMatcher",
    ".*json.*" to "au.com.dius.pact.core.matchers.JsonContentMatcher",
    "text/plain" to "au.com.dius.pact.core.matchers.PlainTextContentMatcher",
    "multipart/.*" to "au.com.dius.pact.core.matchers.MultipartMessageContentMatcher",
    "application/x-www-form-urlencoded" to "au.com.dius.pact.core.matchers.FormPostContentMatcher"
  )

  // Keyed by catalogue entry key (see contentMatcherCatalogueEntries), not by content-type pattern like
  // coreBodyMatchers above - this is what backs the core/content-matcher/<key> capability that plugins can
  // call back into, rather than what pact-jvm's own internal matching dispatch uses.
  private val coreContentMatchers: Map<String, ContentMatcher> = mapOf(
    "xml" to XmlContentMatcher,
    "json" to JsonContentMatcher,
    "text" to PlainTextContentMatcher(),
    "multipart-form-data" to MultipartMessageContentMatcher(),
    "form-urlencoded" to FormPostContentMatcher()
  )

  private val coreContentGenerators: Map<String, CoreContentGenerator> = mapOf(
    "json" to JsonCoreContentGenerator
  )

  @JvmStatic
  fun lookupContentMatcher(contentType: String?): ContentMatcher? {
    return if (contentType != null) {
      val ct = ContentType(contentType)
      val contentMatcher = CatalogueManager.findContentMatcher(ct)
      if (contentMatcher != null) {
        if (!contentMatcher.isCore) {
          PluginContentMatcher(contentMatcher, ct)
        } else {
          coreContentMatcher(contentType)
        }
      } else {
        coreContentMatcher(contentType)
      }
    } else {
      null
    }
  }

  private fun coreContentMatcher(contentType: String): ContentMatcher? {
    return when (val override = System.getProperty("pact.content_type.override.$contentType")) {
      "json" -> JsonContentMatcher
      "text" -> PlainTextContentMatcher()
      is String -> lookupContentMatcher(override)
      else -> {
        val matcher = coreBodyMatchers.entries.find { contentType.matches(Regex(it.key)) }?.value
        if (matcher != null) {
          val clazz = Class.forName(matcher).kotlin
          (clazz.objectInstance ?: clazz.createInstance()) as ContentMatcher?
        } else {
          null
        }
      }
    }
  }

  fun contentMatcherCatalogueEntries(): List<CatalogueEntry> {
    return listOf(
      CatalogueEntry(CatalogueEntryType.CONTENT_MATCHER, CatalogueEntryProviderType.CORE, "core", "xml",
        mapOf(
          "content-types" to "application/.*xml,text/xml",
          "implementation" to "io.pact.core.matchers.XmlBodyMatcher"
        )
      ),
      CatalogueEntry(CatalogueEntryType.CONTENT_MATCHER, CatalogueEntryProviderType.CORE, "core", "json",
        mapOf(
          "content-types" to "application/.*json,application/json-rpc,application/jsonrequest",
          "implementation" to "io.pact.core.matchers.JsonBodyMatcher"
        )
      ),
      CatalogueEntry(CatalogueEntryType.CONTENT_MATCHER, CatalogueEntryProviderType.CORE, "core", "text",
        mapOf(
          "content-types" to "text/plain",
          "implementation" to "io.pact.core.matchers.PlainTextBodyMatcher"
        )
      ),
      CatalogueEntry(CatalogueEntryType.CONTENT_MATCHER, CatalogueEntryProviderType.CORE, "core", "multipart-form-data",
        mapOf(
          "content-types" to "multipart/form-data,multipart/mixed",
          "implementation" to "io.pact.core.matchers.MultipartMessageBodyMatcher"
        )
      ),
      CatalogueEntry(CatalogueEntryType.CONTENT_MATCHER, CatalogueEntryProviderType.CORE, "core", "form-urlencoded",
        mapOf(
          "content-types" to "application/x-www-form-urlencoded",
          "implementation" to "io.pact.core.matchers.FormPostBodyMatcher"
        )
      )
    )
  }

  fun contentHandlerCatalogueEntries(): List<CatalogueEntry> {
    return listOf(
      CatalogueEntry(CatalogueEntryType.CONTENT_GENERATOR, CatalogueEntryProviderType.CORE, "core", "json",
        mapOf(
          "content-types" to "application/.*json,application/json-rpc,application/jsonrequest",
          "implementation" to "au.com.dius.pact.core.model.generators.JsonContentTypeHandler"
        )
      )
    )
  }

  /**
   * Registers the core catalogue entries and the host-provided ("core") capability handlers backing
   * them, so plugins can delegate whole-content-type matching and generation back to this framework's own
   * implementation instead of reproducing it (pact-plugins proposal 009). Safe to call more than once -
   * registration just overwrites the previous entry/handler for a given key.
   *
   * This is the single place all three bootstrap points (the consumer DSLs and the provider verifier)
   * should call, instead of composing and registering the catalogue entry lists themselves.
   */
  fun registerCoreCapabilities() {
    CatalogueManager.registerCoreEntries(
      contentMatcherCatalogueEntries() + matcherCatalogueEntries() + generatorCatalogueEntries() +
        interactionCatalogueEntries() + contentHandlerCatalogueEntries()
    )
    coreContentMatchers.forEach { (key, matcher) ->
      CoreCapabilityRegistry.registerContentMatcher(key, CoreContentMatcherAdapter(matcher))
    }
    coreContentGenerators.forEach { (key, generator) ->
      CoreCapabilityRegistry.registerContentGenerator(key, generator)
    }
    // Field-level: the standard rules and generators applied to a single value, for a plugin that
    // owns a content type but not the rules inside it (proposal 009). The collection-wide ones get
    // a handler that says why they can not be applied one value at a time, rather than no handler.
    FIELD_RULES.forEach { CoreCapabilityRegistry.registerFieldMatcher(it, CoreFieldRuleMatcher) }
    COLLECTION_RULES.forEach { CoreCapabilityRegistry.registerFieldMatcher(it, CollectionRuleMatcher) }
    FIELD_GENERATORS.forEach { CoreCapabilityRegistry.registerFieldGenerator(it, CoreFieldValueGenerator) }
    COLLECTION_GENERATORS.forEach { CoreCapabilityRegistry.registerFieldGenerator(it, CollectionValueGenerator) }
    // The other direction: core:model reaching out to a plugin to apply a field-level rule or
    // generator. It lives here because this is the module that has both the plugin catalogue and
    // the bootstrap that runs before any matching happens (proposal 006).
    PluginSupportRegistry.register(DriverPluginSupport)
  }
}
