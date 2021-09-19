package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.consumer.xml.PactXmlBuilder
import au.com.dius.pact.core.model.OptionalBody.Companion.body
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.queryStringToMap
import au.com.dius.pact.core.support.expressions.DataType
import com.mifmif.common.regex.Generex
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.json.JSONObject
import org.w3c.dom.Document
import java.io.IOException
import java.nio.charset.Charset
import java.util.function.Supplier
import javax.xml.transform.TransformerException

@Suppress("TooManyFunctions")
open class PactDslRequestWithoutPath @JvmOverloads constructor(
  private val consumerPactBuilder: ConsumerPactBuilder,
  private val pactDslWithState: PactDslWithState,
  private val description: String,
  defaultRequestValues: PactDslRequestWithoutPath?,
  private val defaultResponseValues: PactDslResponse?,
  version: PactSpecVersion = PactSpecVersion.V3
) : PactDslRequestBase(defaultRequestValues, pactDslWithState.comments, version) {
  private val consumerName: String = pactDslWithState.consumerName
  private val providerName: String = pactDslWithState.providerName

  init {
    setupDefaultValues()
  }

  /**
   * The HTTP method for the request
   *
   * @param method Valid HTTP method
   */
  fun method(method: String): PactDslRequestWithoutPath {
    requestMethod = method
    return this
  }

  /**
   * Headers to be included in the request
   *
   * @param headers Key-value pairs
   */
  fun headers(headers: Map<String, String>): PactDslRequestWithoutPath {
    for ((key, value) in headers) {
      requestHeaders[key] = listOf(value)
    }
    return this
  }

  /**
   * Headers to be included in the request
   *
   * @param firstHeaderName      The name of the first header
   * @param firstHeaderValue     The value of the first header
   * @param headerNameValuePairs Additional headers in name-value pairs.
   */
  fun headers(
    firstHeaderName: String,
    firstHeaderValue: String,
    vararg headerNameValuePairs: String
  ): PactDslRequestWithoutPath {
    require(headerNameValuePairs.size % 2 == 0) {
      "Pair key value should be provided, but there is one key without value."
    }
    requestHeaders[firstHeaderName] = listOf(firstHeaderValue)
    var i = 0
    while (i < headerNameValuePairs.size) {
      requestHeaders[headerNameValuePairs[i]] = listOf(headerNameValuePairs[i + 1])
      i += 2
    }
    return this
  }

  /**
   * The query string for the request
   *
   * @param query query string
   */
  fun query(query: String): PactDslRequestWithoutPath {
    this.query = queryStringToMap(query, false).toMutableMap()
    return this
  }

  /**
   * The body of the request
   *
   * @param body Request body in string form
   */
  fun body(body: String): PactDslRequestWithoutPath {
    requestBody = body(body.toByteArray())
    return this
  }

  /**
   * The body of the request
   *
   * @param body Request body in string form
   */
  fun body(body: String, contentType: String): PactDslRequestWithoutPath {
    return body(body, ContentType.parse(contentType))
  }

  /**
   * The body of the request
   *
   * @param body Request body in string form
   */
  fun body(body: String, contentType: ContentType): PactDslRequestWithoutPath {
    val charset = if (contentType.charset == null) Charset.defaultCharset() else contentType.charset
    requestBody = body(body.toByteArray(charset), au.com.dius.pact.core.model.ContentType(contentType.toString()))
    requestHeaders[CONTENT_TYPE] = listOf(contentType.toString())
    return this
  }

  /**
   * The body of the request
   *
   * @param body Request body in Java Functional Interface Supplier that must return a string
   */
  fun body(body: Supplier<String>): PactDslRequestWithoutPath {
    requestBody = body(body.get().toByteArray())
    return this
  }

  /**
   * The body of the request
   *
   * @param body Request body in Java Functional Interface Supplier that must return a string
   */
  fun body(body: Supplier<String>, contentType: String): PactDslRequestWithoutPath {
    return this.body(body, ContentType.parse(contentType))
  }

  /**
   * The body of the request
   *
   * @param body Request body in Java Functional Interface Supplier that must return a string
   */
  fun body(body: Supplier<String>, contentType: ContentType): PactDslRequestWithoutPath {
    val charset = if (contentType.charset == null) Charset.defaultCharset() else contentType.charset
    requestBody = body(body.get().toByteArray(charset), au.com.dius.pact.core.model.ContentType(contentType.toString()))
    requestHeaders[CONTENT_TYPE] = listOf(contentType.toString())
    return this
  }

  /**
   * The body of the request with possible single quotes as delimiters
   * and using [QuoteUtil] to convert single quotes to double quotes if required.
   *
   * @param body Request body in string form
   */
  fun bodyWithSingleQuotes(body: String): PactDslRequestWithoutPath {
    return body(QuoteUtil.convert(body))
  }

  /**
   * The body of the request with possible single quotes as delimiters
   * and using [QuoteUtil] to convert single quotes to double quotes if required.
   *
   * @param body Request body in string form
   */
  fun bodyWithSingleQuotes(body: String, contentType: String): PactDslRequestWithoutPath {
    return body(QuoteUtil.convert(body), contentType)
  }

  /**
   * The body of the request with possible single quotes as delimiters
   * and using [QuoteUtil] to convert single quotes to double quotes if required.
   *
   * @param body Request body in string form
   */
  fun bodyWithSingleQuotes(body: String, contentType: ContentType): PactDslRequestWithoutPath {
    return body(QuoteUtil.convert(body), contentType)
  }

  /**
   * The body of the request
   *
   * @param body Request body in JSON form
   */
  fun body(body: JSONObject): PactDslRequestWithoutPath {
    if (isContentTypeHeaderNotSet) {
      requestHeaders[CONTENT_TYPE] = listOf(ContentType.APPLICATION_JSON.toString())
      requestBody = body(body.toString().toByteArray())
    } else {
      val contentType = contentTypeHeader
      val ct = ContentType.parse(contentType)
      val charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
      requestBody = body(body.toString().toByteArray(charset),
        au.com.dius.pact.core.model.ContentType(contentType))
    }
    return this
  }

  /**
   * The body of the request
   *
   * @param body Built using the Pact body DSL
   */
  fun body(body: DslPart): PactDslRequestWithoutPath {
    val parent = body.close()

    requestMatchers.addCategory(parent!!.matchers)
    requestGenerators.addGenerators(parent.generators)

    if (isContentTypeHeaderNotSet) {
      requestHeaders[CONTENT_TYPE] = listOf(ContentType.APPLICATION_JSON.toString())
      requestBody = body(parent.toString().toByteArray())
    } else {
      val contentType = contentTypeHeader
      val ct = ContentType.parse(contentType)
      val charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
      requestBody = body(parent.toString().toByteArray(charset),
        au.com.dius.pact.core.model.ContentType(contentType))
    }
    return this
  }

  /**
   * The body of the request
   *
   * @param body XML Document
   */
  @Throws(TransformerException::class)
  fun body(body: Document): PactDslRequestWithoutPath {
    if (isContentTypeHeaderNotSet) {
      requestHeaders[CONTENT_TYPE] = listOf(ContentType.APPLICATION_XML.toString())
      requestBody = body(ConsumerPactBuilder.xmlToString(body).toByteArray())
    } else {
      val contentType = contentTypeHeader
      val ct = ContentType.parse(contentType)
      val charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
      requestBody = body(ConsumerPactBuilder.xmlToString(body).toByteArray(charset),
        au.com.dius.pact.core.model.ContentType(contentType))
    }
    return this
  }

  /**
   * XML Response body to return
   *
   * @param xmlBuilder XML Builder used to construct the XML document
   */
  fun body(xmlBuilder: PactXmlBuilder): PactDslRequestWithoutPath {
    requestMatchers.addCategory(xmlBuilder.matchingRules)
    requestGenerators.addGenerators(xmlBuilder.generators)
    if (isContentTypeHeaderNotSet) {
      requestHeaders[CONTENT_TYPE] = listOf(ContentType.APPLICATION_XML.toString())
      requestBody = body(xmlBuilder.asBytes())
    } else {
      val contentType = contentTypeHeader
      val ct = ContentType.parse(contentType)
      val charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
      requestBody = body(xmlBuilder.asBytes(charset),
        au.com.dius.pact.core.model.ContentType(contentType))
    }
    return this
  }

  /**
   * The body of the request
   *
   * @param body Built using MultipartEntityBuilder
   */
  open fun body(body: MultipartEntityBuilder): PactDslRequestWithoutPath {
    setupMultipart(body)
    return this
  }

  /**
   * The path of the request
   *
   * @param path string path
   */
  fun path(path: String): PactDslRequestWithPath {
    return PactDslRequestWithPath(consumerPactBuilder, consumerName, providerName, pactDslWithState.state,
      description, path, requestMethod, requestHeaders, query, requestBody, requestMatchers, requestGenerators,
      defaultRequestValues, defaultResponseValues, comments, version)
  }

  /**
   * The path of the request. This will generate a random path to use when generating requests if the example
   * value is not provided.
   *
   * @param path      string path to use when generating requests
   * @param pathRegex regular expression to use to match paths
   */
  /**
   * The path of the request. This will generate a random path to use when generating requests
   *
   * @param pathRegex string path regular expression to match with
   */
  @JvmOverloads
  fun matchPath(pathRegex: String, path: String = Generex(pathRegex).random()): PactDslRequestWithPath {
    requestMatchers.addCategory("path").addRule(RegexMatcher(pathRegex))
    return PactDslRequestWithPath(consumerPactBuilder, consumerName, providerName, pactDslWithState.state,
      description, path, requestMethod, requestHeaders, query, requestBody, requestMatchers, requestGenerators,
      defaultRequestValues, defaultResponseValues, comments, version)
  }

  /**
   * Sets up a file upload request. This will add the correct content type header to the request
   * @param partName This is the name of the part in the multipart body.
   * @param fileName This is the name of the file that was uploaded
   * @param fileContentType This is the content type of the uploaded file
   * @param data This is the actual file contents
   */
  @Throws(IOException::class)
  fun withFileUpload(
    partName: String,
    fileName: String,
    fileContentType: String?,
    data: ByteArray
  ): PactDslRequestWithoutPath {
    setupFileUpload(partName, fileName, fileContentType, data)
    return this
  }

  /**
   * Adds a header that will have it's value injected from the provider state
   * @param name Header Name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  fun headerFromProviderState(name: String, expression: String, example: String): PactDslRequestWithoutPath {
    requestGenerators.addGenerator(Category.HEADER, name, ProviderStateGenerator(expression, DataType.STRING))
    requestHeaders[name] = listOf(example)
    return this
  }

  /**
   * Adds a query parameter that will have it's value injected from the provider state
   * @param name Name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  fun queryParameterFromProviderState(name: String, expression: String, example: String): PactDslRequestWithoutPath {
    requestGenerators.addGenerator(Category.QUERY, name, ProviderStateGenerator(expression, DataType.STRING))
    query[name] = listOf(example)
    return this
  }

  /**
   * Sets the path to have it's value injected from the provider state
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  fun pathFromProviderState(expression: String, example: String): PactDslRequestWithPath {
    requestGenerators.addGenerator(Category.PATH, "", ProviderStateGenerator(expression, DataType.STRING))
    return PactDslRequestWithPath(consumerPactBuilder, consumerName, providerName, pactDslWithState.state,
      description, example, requestMethod, requestHeaders, query, requestBody, requestMatchers, requestGenerators,
      defaultRequestValues, defaultResponseValues, comments, version)
  }

  /**
   * Matches a date field using the provided date pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  fun queryMatchingDate(field: String, pattern: String, example: String): PactDslRequestWithoutPath {
    return queryMatchingDateBase(field, pattern, example) as PactDslRequestWithoutPath
  }

  /**
   * Matches a date field using the provided date pattern. The current system date will be used for the example value.
   * @param field field name
   * @param pattern pattern to match
   */
  fun queryMatchingDate(field: String, pattern: String): PactDslRequestWithoutPath {
    return queryMatchingDateBase(field, pattern, null) as PactDslRequestWithoutPath
  }

  /**
   * Matches a time field using the provided time pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  fun queryMatchingTime(field: String, pattern: String, example: String): PactDslRequestWithoutPath {
    return queryMatchingTimeBase(field, pattern, example) as PactDslRequestWithoutPath
  }

  /**
   * Matches a time field using the provided time pattern. The current system time will be used for the example value.
   * @param field field name
   * @param pattern pattern to match
   */
  fun queryMatchingTime(field: String, pattern: String): PactDslRequestWithoutPath {
    return queryMatchingTimeBase(field, pattern, null) as PactDslRequestWithoutPath
  }

  /**
   * Matches a datetime field using the provided pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  fun queryMatchingDatetime(field: String, pattern: String, example: String): PactDslRequestWithoutPath {
    return queryMatchingDatetimeBase(field, pattern, example) as PactDslRequestWithoutPath
  }

  /**
   * Matches a datetime field using the provided pattern. The current system date and time will be used for the
   * example value.
   * @param field field name
   * @param pattern pattern to match
   */
  fun queryMatchingDatetime(field: String, pattern: String): PactDslRequestWithoutPath {
    return queryMatchingDatetimeBase(field, pattern, null) as PactDslRequestWithoutPath
  }

  /**
   * Matches a date field using the ISO date pattern.  The current system date will be used for the example value
   * if not provided.
   * @param field field name
   * @param example Example value
   */
  @JvmOverloads
  fun queryMatchingISODate(field: String, example: String? = null): PactDslRequestWithoutPath {
    return queryMatchingDateBase(field, DateFormatUtils.ISO_DATE_FORMAT.pattern, example) as PactDslRequestWithoutPath
  }

  /**
   * Matches a time field using the ISO time pattern
   * @param field field name
   * @param example Example value
   */
  fun queryMatchingISOTime(field: String, example: String?): PactDslRequestWithoutPath {
    return queryMatchingTimeBase(field, DateFormatUtils.ISO_TIME_FORMAT.pattern, example) as PactDslRequestWithoutPath
  }

  /**
   * Matches a time field using the ISO time pattern. The current system time will be used for the example value.
   * @param field field name
   */
  fun queryMatchingTime(field: String): PactDslRequestWithoutPath {
    return queryMatchingISOTime(field, null)
  }

  /**
   * Matches a datetime field using the ISO pattern. The current system date and time will be used for the example
   * value if not provided.
   * @param field field name
   * @param example Example value
   */
  @JvmOverloads
  fun queryMatchingISODatetime(field: String, example: String? = null): PactDslRequestWithoutPath {
    return queryMatchingDatetimeBase(field, DateFormatUtils.ISO_DATETIME_FORMAT.pattern,
      example) as PactDslRequestWithoutPath
  }
}
