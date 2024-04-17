package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.Headers.MULTIPART_HEADER_REGEX
import au.com.dius.pact.core.model.OptionalBody.Companion.body
import au.com.dius.pact.core.model.OptionalBody.Companion.missing
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.DateTimeGenerator
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.TimeGenerator
import au.com.dius.pact.core.model.matchingrules.ContentTypeMatcher
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.support.isNotEmpty
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.core5.http.ContentType
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.Date

open class PactDslRequestBase(
  protected val defaultRequestValues: PactDslRequestWithoutPath?,
  var comments: MutableList<String> = mutableListOf(),
  var version: PactSpecVersion = PactSpecVersion.V3
) {
  @JvmField
  var requestMethod = "GET"
  @JvmField
  var requestHeaders: MutableMap<String, List<String>> = mutableMapOf()
  @JvmField
  var query: MutableMap<String, List<String?>> = mutableMapOf()
  @JvmField
  var requestBody = missing()
  @JvmField
  var requestMatchers: MatchingRules = MatchingRulesImpl()
  @JvmField
  var requestGenerators = Generators()

  var multipartBuilder: MultipartEntityBuilder? = null

  protected fun setupDefaultValues() {
    if (defaultRequestValues != null) {
      if (StringUtils.isNotEmpty(defaultRequestValues.requestMethod)) {
        requestMethod = defaultRequestValues.requestMethod
      }
      requestHeaders.putAll(defaultRequestValues.requestHeaders)
      query.putAll(defaultRequestValues.query)
      requestBody = defaultRequestValues.requestBody
      requestMatchers = (defaultRequestValues.requestMatchers as MatchingRulesImpl).copy()
      requestGenerators = Generators(defaultRequestValues.requestGenerators.categories)
    }
  }

  @Throws(IOException::class)
  protected fun setupFileUpload(
    partName: String,
    fileName: String,
    fileContentType: String?,
    data: ByteArray
  ) {
    val contentType = if (fileContentType.isNotEmpty())
      ContentType.create(fileContentType)
    else
      ContentType.DEFAULT_TEXT
    if (multipartBuilder == null) {
      multipartBuilder = MultipartEntityBuilder.create()
        .setMode(HttpMultipartMode.EXTENDED)
        .addBinaryBody(partName, data, contentType, fileName)
    } else {
      multipartBuilder!!.addBinaryBody(partName, data, contentType, fileName)
    }
    setupMultipart(multipartBuilder!!)
  }

  fun setupMultipart(multipart: MultipartEntityBuilder) {
    val entity = multipart.build()
    val os = ByteArrayOutputStream()
    entity.writeTo(os)
    requestBody = body(os.toByteArray(), au.com.dius.pact.core.model.ContentType(entity.contentType))
    val matchingRuleCategory = requestMatchers.addCategory("header")
    if (!matchingRuleCategory.matchingRules.containsKey(CONTENT_TYPE)) {
      matchingRuleCategory.addRule(CONTENT_TYPE, RegexMatcher(MULTIPART_HEADER_REGEX,
        entity.contentType))
    }
    if (!requestHeaders.containsKey(CONTENT_TYPE)) {
      requestHeaders[CONTENT_TYPE] = listOf(entity.contentType)
    }
  }

  protected fun queryMatchingDateBase(field: String, pattern: String?, example: String?): PactDslRequestBase {
    requestMatchers.addCategory("query").addRule(field, DateMatcher(pattern!!))
    if (example.isNotEmpty()) {
      query[field] = listOf(example!!)
    } else {
      requestGenerators.addGenerator(Category.QUERY, field, DateGenerator(pattern, null))
      val instance = FastDateFormat.getInstance(pattern)
      query[field] = listOf(instance.format(Date(DATE_2000)))
    }
    return this
  }

  protected fun queryMatchingTimeBase(field: String, pattern: String?, example: String?): PactDslRequestBase {
    requestMatchers.addCategory("query").addRule(field, TimeMatcher(pattern!!))
    if (example.isNotEmpty()) {
      query[field] = listOf(example!!)
    } else {
      requestGenerators.addGenerator(Category.QUERY, field, TimeGenerator(pattern, null))
      val instance = FastDateFormat.getInstance(pattern)
      query[field] = listOf(instance.format(Date(DATE_2000)))
    }
    return this
  }

  protected fun queryMatchingDatetimeBase(field: String, pattern: String?, example: String?): PactDslRequestBase {
    requestMatchers.addCategory("query").addRule(field, TimestampMatcher(pattern!!))
    if (example.isNotEmpty()) {
      query[field] = listOf(example!!)
    } else {
      requestGenerators.addGenerator(Category.QUERY, field, DateTimeGenerator(pattern, null))
      val instance = FastDateFormat.getInstance(pattern)
      query[field] = listOf(instance.format(Date(DATE_2000)))
    }
    return this
  }

  /**
   * Sets up a content type matcher to match any body of the given content type
   */
  protected open fun bodyMatchingContentType(contentType: String, exampleContents: String): PactDslRequestBase {
    val ct = au.com.dius.pact.core.model.ContentType(contentType)
    val charset = ct.asCharset()
    requestBody = body(exampleContents.toByteArray(charset), ct)
    requestHeaders[CONTENT_TYPE] = listOf(contentType)
    requestMatchers.addCategory("body").addRule("$", ContentTypeMatcher(contentType))
    return this
  }

  protected val isContentTypeHeaderNotSet: Boolean
    get() = requestHeaders.keys.none { key -> key.equals(CONTENT_TYPE, ignoreCase = true) }
  protected val contentTypeHeader: String
    get() = requestHeaders.entries.find { entry -> entry.key.equals(CONTENT_TYPE, ignoreCase = true) }
      ?.value?.get(0) ?: ""

  companion object {
    const val CONTENT_TYPE = "Content-Type"
    const val DATE_2000 = 949323600000L
  }
}
