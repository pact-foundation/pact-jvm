package au.com.dius.pact.core.matchers

import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType
import kotlin.reflect.full.createInstance

object MatchingConfig {
  private val coreBodyMatchers = mapOf(
    "application/.*xml" to "au.com.dius.pact.core.matchers.XmlBodyMatcher",
    "text/xml" to "au.com.dius.pact.core.matchers.XmlBodyMatcher",
    ".*json.*" to "au.com.dius.pact.core.matchers.JsonBodyMatcher",
    "text/plain" to "au.com.dius.pact.core.matchers.PlainTextBodyMatcher",
    "multipart/form-data" to "au.com.dius.pact.core.matchers.MultipartMessageBodyMatcher",
    "multipart/mixed" to "au.com.dius.pact.core.matchers.MultipartMessageBodyMatcher",
    "application/x-www-form-urlencoded" to "au.com.dius.pact.core.matchers.FormPostBodyMatcher"
  )

  @JvmStatic
  fun lookupContentMatcher(contentType: String?): ContentMatcher? {
    return if (contentType != null) {
      val matcher = coreBodyMatchers.entries.find { contentType.matches(Regex(it.key)) }?.value
      if (matcher != null) {
        val clazz = Class.forName(matcher).kotlin
        (clazz.objectInstance ?: clazz.createInstance()) as ContentMatcher?
      } else {
        when (System.getProperty("pact.content_type.override.$contentType")) {
          "json" -> JsonContentMatcher
          "text" -> PlainTextContentMatcher()
          else -> null
        }
      }
    } else {
      null
    }
  }

  fun contentMatcherCatalogueEntries(): List<CatalogueEntry> {
    return listOf(
      CatalogueEntry(CatalogueEntryType.CONTENT_MATCHER, CatalogueEntryProviderType.CORE, "xml",
        mapOf(
          "content-types" to "application/.*xml,text/xml",
          "implementation" to "io.pact.core.matchers.XmlBodyMatcher"
        )
      ),
      CatalogueEntry(CatalogueEntryType.CONTENT_MATCHER, CatalogueEntryProviderType.CORE, "json",
        mapOf(
          "content-types" to "application/.*json,application/json-rpc,application/jsonrequest",
          "implementation" to "io.pact.core.matchers.JsonBodyMatcher"
        )
      ),
      CatalogueEntry(CatalogueEntryType.CONTENT_MATCHER, CatalogueEntryProviderType.CORE, "text",
        mapOf(
          "content-types" to "text/plain",
          "implementation" to "io.pact.core.matchers.PlainTextBodyMatcher"
        )
      ),
      CatalogueEntry(CatalogueEntryType.CONTENT_MATCHER, CatalogueEntryProviderType.CORE, "multipart-form-data",
        mapOf(
          "content-types" to "multipart/form-data,multipart/mixed",
          "implementation" to "io.pact.core.matchers.MultipartMessageBodyMatcher"
        )
      ),
      CatalogueEntry(CatalogueEntryType.CONTENT_MATCHER, CatalogueEntryProviderType.CORE, "form-urlencoded",
        mapOf(
          "content-types" to "application/x-www-form-urlencoded",
          "implementation" to "io.pact.core.matchers.FormPostBodyMatcher"
        )
      )
    )
  }
}
