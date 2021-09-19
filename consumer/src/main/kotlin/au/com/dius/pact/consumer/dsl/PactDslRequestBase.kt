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
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.support.isNotEmpty
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import java.io.ByteArrayOutputStream
import java.io.IOException
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
  var query: MutableMap<String, List<String>> = mutableMapOf()
  @JvmField
  var requestBody = missing()
  @JvmField
  var requestMatchers: MatchingRules = MatchingRulesImpl()
  @JvmField
  var requestGenerators = Generators()

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
    val multipart = MultipartEntityBuilder.create()
      .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
      .addBinaryBody(partName, data, contentType, fileName)
    setupMultipart(multipart)
  }

  fun setupMultipart(multipart: MultipartEntityBuilder) {
    val entity = multipart.build()
    val os = ByteArrayOutputStream()
    entity.writeTo(os)
    requestBody = body(os.toByteArray(),
      au.com.dius.pact.core.model.ContentType(entity.contentType.value))
    requestMatchers.addCategory("header").addRule(CONTENT_TYPE, RegexMatcher(MULTIPART_HEADER_REGEX,
      entity.contentType.value))
    requestHeaders[CONTENT_TYPE] = listOf(entity.contentType.value)
  }

  protected fun queryMatchingDateBase(field: String, pattern: String?, example: String?): PactDslRequestBase {
    requestMatchers.addCategory("query").addRule(field, DateMatcher(pattern!!))
    if (example.isNotEmpty()) {
      query[field] = listOf(example!!)
    } else {
      requestGenerators.addGenerator(Category.BODY, field, DateGenerator(pattern, null))
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
      requestGenerators.addGenerator(Category.BODY, field, TimeGenerator(pattern, null))
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
      requestGenerators.addGenerator(Category.BODY, field, DateTimeGenerator(pattern, null))
      val instance = FastDateFormat.getInstance(pattern)
      query[field] = listOf(instance.format(Date(DATE_2000)))
    }
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
