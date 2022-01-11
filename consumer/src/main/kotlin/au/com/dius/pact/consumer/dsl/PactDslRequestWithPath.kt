package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.consumer.xml.PactXmlBuilder
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType.Companion.JSON
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.OptionalBody.Companion.body
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.queryStringToMap
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.json.JsonValue
import com.mifmif.common.regex.Generex
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.core5.http.ContentType
import org.json.JSONObject
import org.w3c.dom.Document
import java.io.IOException
import java.nio.charset.Charset
import java.util.function.Supplier
import javax.xml.transform.TransformerException

@Suppress("TooManyFunctions")
open class PactDslRequestWithPath : PactDslRequestBase {
  private val consumerPactBuilder: ConsumerPactBuilder
  @JvmField
  var consumer: Consumer
  @JvmField
  var provider: Provider
  @JvmField
  var state: List<ProviderState>
  @JvmField
  var description: String
  var path = "/"
  private val defaultResponseValues: PactDslResponse?
  private var additionalMetadata: MutableMap<String, JsonValue> = mutableMapOf()

  @Suppress("LongParameterList")
  @JvmOverloads
  internal constructor(consumerPactBuilder: ConsumerPactBuilder,
                       consumerName: String,
                       providerName: String,
                       state: List<ProviderState>,
                       description: String,
                       path: String,
                       requestMethod: String,
                       requestHeaders: MutableMap<String, List<String>>,
                       query: MutableMap<String, List<String>>,
                       requestBody: OptionalBody,
                       requestMatchers: MatchingRules,
                       requestGenerators: Generators,
                       defaultRequestValues: PactDslRequestWithoutPath?,
                       defaultResponseValues: PactDslResponse?,
                       comments: MutableList<String> = mutableListOf(),
                       version: PactSpecVersion = PactSpecVersion.V3,
                       additionalMetadata: MutableMap<String, JsonValue> = mutableMapOf()
  ) : super(defaultRequestValues, comments, version) {
    this.consumerPactBuilder = consumerPactBuilder
    this.requestMatchers = requestMatchers
    consumer = Consumer(consumerName)
    provider = Provider(providerName)
    this.state = state
    this.description = description
    this.path = path
    this.requestMethod = requestMethod
    this.requestHeaders = requestHeaders
    this.query = query
    this.requestBody = requestBody
    this.requestMatchers = requestMatchers
    this.requestGenerators = requestGenerators
    this.defaultResponseValues = defaultResponseValues
    this.comments = comments
    this.additionalMetadata = additionalMetadata
    setupDefaultValues()
  }

  @JvmOverloads
  @Suppress("LongParameterList")
  internal constructor(
    consumerPactBuilder: ConsumerPactBuilder,
    existing: PactDslRequestWithPath,
    description: String,
    defaultRequestValues: PactDslRequestWithoutPath?,
    defaultResponseValues: PactDslResponse?,
    comments: MutableList<String> = mutableListOf(),
    version: PactSpecVersion = PactSpecVersion.V3,
    additionalMetadata: MutableMap<String, JsonValue> = mutableMapOf()
  ) : super(defaultRequestValues, comments, version) {
    requestMethod = "GET"
    this.consumerPactBuilder = consumerPactBuilder
    consumer = existing.consumer
    provider = existing.provider
    state = mutableListOf()
    this.description = description
    this.defaultResponseValues = defaultResponseValues
    path = existing.path
    this.additionalMetadata = additionalMetadata
    setupDefaultValues()
  }

  /**
   * The HTTP method for the request
   *
   * @param method Valid HTTP method
   */
  fun method(method: String): PactDslRequestWithPath {
    requestMethod = method
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
  ): PactDslRequestWithPath {
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
   * Headers to be included in the request
   *
   * @param headers Key-value pairs
   */
  fun headers(headers: Map<String, String>): PactDslRequestWithPath {
    for ((key, value) in headers) {
      requestHeaders[key] = listOf(value)
    }
    return this
  }

  /**
   * The query string for the request
   *
   * @param query query string
   */
  fun query(query: String): PactDslRequestWithPath {
    this.query = queryStringToMap(query, false).toMutableMap()
    return this
  }

  /**
   * The encoded query string for the request
   *
   * @param query query string
   */
  fun encodedQuery(query: String): PactDslRequestWithPath {
    this.query = queryStringToMap(query, true).toMutableMap()
    return this
  }

  /**
   * The body of the request
   *
   * @param body Request body in string form
   */
  fun body(body: String): PactDslRequestWithPath {
    requestBody = body(body.toByteArray())
    return this
  }

  /**
   * The body of the request
   *
   * @param body Request body in string form
   */
  fun body(body: String, contentType: String): PactDslRequestWithPath {
    return body(body, ContentType.parse(contentType))
  }

  /**
   * The body of the request
   *
   * @param body Request body in string form
   */
  fun body(body: String, contentType: ContentType): PactDslRequestWithPath {
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
  fun body(body: Supplier<String>): PactDslRequestWithPath {
    requestBody = body(body.get().toByteArray())
    return this
  }

  /**
   * The body of the request
   *
   * @param body Request body in Java Functional Interface Supplier that must return a string
   */
  fun body(body: Supplier<String>, contentType: String): PactDslRequestWithPath {
    return body(body, ContentType.parse(contentType))
  }

  /**
   * The body of the request
   *
   * @param body Request body in Java Functional Interface Supplier that must return a string
   */
  fun body(body: Supplier<String>, contentType: ContentType): PactDslRequestWithPath {
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
  fun bodyWithSingleQuotes(body: String): PactDslRequestWithPath {
    return body(QuoteUtil.convert(body))
  }

  /**
   * The body of the request with possible single quotes as delimiters
   * and using [QuoteUtil] to convert single quotes to double quotes if required.
   *
   * @param body Request body in string form
   */
  fun bodyWithSingleQuotes(body: String, contentType: String): PactDslRequestWithPath {
    return body(QuoteUtil.convert(body), contentType)
  }

  /**
   * The body of the request with possible single quotes as delimiters
   * and using [QuoteUtil] to convert single quotes to double quotes if required.
   *
   * @param body Request body in string form
   */
  fun bodyWithSingleQuotes(body: String, contentType: ContentType): PactDslRequestWithPath {
    return body(QuoteUtil.convert(body), contentType)
  }

  /**
   * The body of the request
   *
   * @param body Request body in JSON form
   */
  fun body(body: JSONObject): PactDslRequestWithPath {
    requestBody = body(body.toString().toByteArray(), JSON)
    if (isContentTypeHeaderNotSet) {
      requestHeaders[CONTENT_TYPE] = listOf(ContentType.APPLICATION_JSON.toString())
      requestBody = body(body.toString().toByteArray())
    } else {
      val contentType = ContentType.parse(contentTypeHeader)
      val charset = if (contentType.charset != null) contentType.charset else Charset.defaultCharset()
      requestBody = body(body.toString().toByteArray(charset),
        au.com.dius.pact.core.model.ContentType(contentType.toString()))
    }
    return this
  }

  /**
   * The body of the request
   *
   * @param body Built using the Pact body DSL
   */
  fun body(body: DslPart): PactDslRequestWithPath {
    val parent = body.close()!!
    requestMatchers.addCategory(parent.matchers)
    requestGenerators.addGenerators(parent.generators)
    var charset = Charset.defaultCharset()
    var contentType = ContentType.APPLICATION_JSON.toString()
    if (isContentTypeHeaderNotSet) {
      requestHeaders[CONTENT_TYPE] = listOf(contentType)
    } else {
      contentType = contentTypeHeader
      val ct = ContentType.parse(contentType)
      charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
    }
    requestBody = body(parent.toString().toByteArray(charset),
      au.com.dius.pact.core.model.ContentType(contentType))
    return this
  }

  /**
   * The body of the request
   *
   * @param body XML Document
   */
  @Throws(TransformerException::class)
  fun body(body: Document): PactDslRequestWithPath {
    if (isContentTypeHeaderNotSet) {
      val contentType = ContentType.APPLICATION_XML.toString()
      requestHeaders[CONTENT_TYPE] = listOf(contentType)
      requestBody = body(ConsumerPactBuilder.xmlToString(body).toByteArray(),
        au.com.dius.pact.core.model.ContentType(contentType))
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
   * XML body to return
   *
   * @param xmlBuilder XML Builder used to construct the XML document
   */
  fun body(xmlBuilder: PactXmlBuilder): PactDslRequestWithPath {
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
  open fun body(body: MultipartEntityBuilder): PactDslRequestWithPath {
    setupMultipart(body)
    return this
  }

  /**
   * The path of the request
   *
   * @param path string path
   */
  fun path(path: String): PactDslRequestWithPath {
    this.path = path
    return this
  }
  /**
   * The path of the request. This will generate a random path to use when generating requests if
   * the example value is not provided.
   *
   * @param path      string path to use when generating requests
   * @param pathRegex regular expression to use to match paths
   */
  @JvmOverloads
  fun matchPath(pathRegex: String, path: String = Generex(pathRegex).random()): PactDslRequestWithPath {
    requestMatchers.addCategory("path").addRule(RegexMatcher(pathRegex))
    this.path = path
    return this
  }

  /**
   * Match a request header. A random example header value will be generated from the provided regular expression
   * if the example value is not provided.
   *
   * @param header        Header to match
   * @param regex         Regular expression to match
   * @param headerExample Example value to use
   */
  @JvmOverloads
  fun matchHeader(
    header: String,
    regex: String,
    headerExample: String = Generex(regex).random()
  ): PactDslRequestWithPath {
    requestMatchers.addCategory("header").setRule(header, RegexMatcher(regex))
    requestHeaders[header] = listOf(headerExample)
    return this
  }

  /**
   * Define the response to return
   */
  fun willRespondWith(): PactDslResponse {
    return PactDslResponse(consumerPactBuilder, this, defaultRequestValues, defaultResponseValues, comments,
      version, additionalMetadata)
  }

  /**
   * Match a query parameter with a regex. A random query parameter value will be generated from the regex
   * if the example value is not provided.
   *
   * @param parameter Query parameter
   * @param regex     Regular expression to match with
   * @param example   Example value to use for the query parameter (unencoded)
   */
  @JvmOverloads
  fun matchQuery(
    parameter: String,
    regex: String,
    example: String = Generex(regex).random()
  ): PactDslRequestWithPath {
    requestMatchers.addCategory("query").addRule(parameter, RegexMatcher(regex))
    query[parameter] = listOf(example)
    return this
  }

  /**
   * Match a repeating query parameter with a regex.
   *
   * @param parameter Query parameter
   * @param regex     Regular expression to match with each element
   * @param example   Example value list to use for the query parameter (unencoded)
   */
  fun matchQuery(parameter: String, regex: String, example: List<String>): PactDslRequestWithPath {
    requestMatchers.addCategory("query").addRule(parameter, RegexMatcher(regex))
    query[parameter] = example
    return this
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
  ): PactDslRequestWithPath {
    setupFileUpload(partName, fileName, fileContentType, data)
    return this
  }

  /**
   * Adds a header that will have it's value injected from the provider state
   * @param name Header Name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  fun headerFromProviderState(name: String, expression: String, example: String): PactDslRequestWithPath {
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
  fun queryParameterFromProviderState(name: String, expression: String, example: String): PactDslRequestWithPath {
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
    path = example
    return this
  }

  /**
   * Matches a date field using the provided date pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  fun queryMatchingDate(field: String, pattern: String, example: String): PactDslRequestWithPath {
    return queryMatchingDateBase(field, pattern, example) as PactDslRequestWithPath
  }

  /**
   * Matches a date field using the provided date pattern. The current system date will be used for the example value.
   * @param field field name
   * @param pattern pattern to match
   */
  fun queryMatchingDate(field: String, pattern: String): PactDslRequestWithPath {
    return queryMatchingDateBase(field, pattern, null) as PactDslRequestWithPath
  }

  /**
   * Matches a time field using the provided time pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  fun queryMatchingTime(field: String, pattern: String, example: String): PactDslRequestWithPath {
    return queryMatchingTimeBase(field, pattern, example) as PactDslRequestWithPath
  }

  /**
   * Matches a time field using the provided time pattern. The current system time will be used for the example value.
   * @param field field name
   * @param pattern pattern to match
   */
  fun queryMatchingTime(field: String, pattern: String): PactDslRequestWithPath {
    return queryMatchingTimeBase(field, pattern, null) as PactDslRequestWithPath
  }

  /**
   * Matches a datetime field using the provided pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  fun queryMatchingDatetime(field: String, pattern: String, example: String): PactDslRequestWithPath {
    return queryMatchingDatetimeBase(field, pattern, example) as PactDslRequestWithPath
  }

  /**
   * Matches a datetime field using the provided pattern. The current system date and time will be used for
   * the example value.
   * @param field field name
   * @param pattern pattern to match
   */
  fun queryMatchingDatetime(field: String, pattern: String): PactDslRequestWithPath {
    return queryMatchingDatetimeBase(field, pattern, null) as PactDslRequestWithPath
  }
  /**
   * Matches a date field using the ISO date pattern
   * @param field field name
   * @param example Example value
   */
  /**
   * Matches a date field using the ISO date pattern. The current system date will be used for the example value.
   * @param field field name
   */
  @JvmOverloads
  fun queryMatchingISODate(field: String, example: String? = null): PactDslRequestWithPath {
    return queryMatchingDateBase(field, DateFormatUtils.ISO_DATE_FORMAT.pattern, example) as PactDslRequestWithPath
  }

  /**
   * Matches a time field using the ISO time pattern
   * @param field field name
   * @param example Example value
   */
  fun queryMatchingISOTime(field: String, example: String?): PactDslRequestWithPath {
    return queryMatchingTimeBase(field, DateFormatUtils.ISO_TIME_NO_T_FORMAT.pattern, example) as PactDslRequestWithPath
  }

  /**
   * Matches a time field using the ISO time pattern. The current system time will be used for the example value.
   * @param field field name
   */
  fun queryMatchingTime(field: String): PactDslRequestWithPath {
    return queryMatchingISOTime(field, null)
  }

  /**
   * Matches a datetime field using the ISO pattern. The current system date and time will be used for the example
   * value if not provided.
   * @param field field name
   * @param example Example value
   */
  @JvmOverloads
  fun queryMatchingISODatetime(field: String, example: String? = null): PactDslRequestWithPath {
    return queryMatchingDatetimeBase(field, "yyyy-MM-dd'T'HH:mm:ssXXX", example) as PactDslRequestWithPath
  }

  /**
   * Sets the body using the buidler
   * @param builder Body Builder
   */
  fun body(builder: BodyBuilder): PactDslRequestWithPath {
    requestMatchers.addCategory(builder.matchers)
    requestGenerators.addGenerators(builder.generators)
    val contentType = builder.contentType
    requestHeaders[CONTENT_TYPE] = listOf(contentType.toString())
    requestBody = body(builder.buildBody(), contentType)
    return this
  }

  /**
   * Adds a comment to this interaction
   */
  fun comment(comment: String): PactDslRequestWithPath {
    this.comments.add(comment)
    return this
  }

  /**
   * Adds additional values to the metadata section of the Pact file
   */
  fun addMetadataValue(key: String, value: String): PactDslRequestWithPath {
    additionalMetadata[key] = JsonValue.StringValue(value)
    return this
  }

  /**
   * Adds additional values to the metadata section of the Pact file
   */
  fun addMetadataValue(key: String, value: JsonValue): PactDslRequestWithPath {
    additionalMetadata[key] = value
    return this
  }
}
