package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType
import io.pact.plugins.jvm.core.CatalogueManager
import kotlin.reflect.full.createInstance

object MatchingConfig {
  private val coreBodyMatchers = mapOf(
    "application/vnd.schemaregistry.v1\\+json" to "au.com.dius.pact.core.matchers.KafkaJsonSchemaContentMatcher",
    "application/.*xml" to "au.com.dius.pact.core.matchers.XmlContentMatcher",
    "text/xml" to "au.com.dius.pact.core.matchers.XmlContentMatcher",
    ".*json.*" to "au.com.dius.pact.core.matchers.JsonContentMatcher",
    "text/plain" to "au.com.dius.pact.core.matchers.PlainTextContentMatcher",
    "multipart/form-data" to "au.com.dius.pact.core.matchers.MultipartMessageContentMatcher",
    "multipart/mixed" to "au.com.dius.pact.core.matchers.MultipartMessageContentMatcher",
    "application/x-www-form-urlencoded" to "au.com.dius.pact.core.matchers.FormPostContentMatcher"
  )

  @JvmStatic
  fun lookupContentMatcher(contentType: String?): ContentMatcher? {
    return if (contentType != null) {
      val contentType1 = ContentType(contentType)
      val contentMatcher = CatalogueManager.findContentMatcher(contentType1)
      if (contentMatcher != null) {
        if (!contentMatcher.isCore) {
          PluginContentMatcher(contentMatcher, contentType1)
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
    val matcher = coreBodyMatchers.entries.find { contentType.matches(Regex(it.key)) }?.value
    return if (matcher != null) {
      val clazz = Class.forName(matcher).kotlin
      (clazz.objectInstance ?: clazz.createInstance()) as ContentMatcher?
    } else {
      when (System.getProperty("pact.content_type.override.$contentType")) {
        "json" -> JsonContentMatcher
        "text" -> PlainTextContentMatcher()
        else -> null
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
}
