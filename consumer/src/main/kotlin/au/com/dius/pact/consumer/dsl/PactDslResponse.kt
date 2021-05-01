package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.consumer.xml.PactXmlBuilder
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.BasePact.Companion.DEFAULT_METADATA
import au.com.dius.pact.core.model.ContentType.Companion.fromString
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.HttpResponse
import au.com.dius.pact.core.model.OptionalBody.Companion.body
import au.com.dius.pact.core.model.OptionalBody.Companion.missing
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.SynchronousRequestResponse
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.matchingrules.ContentTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.jsonArray
import com.mifmif.common.regex.Generex
import org.apache.http.entity.ContentType
import org.json.JSONObject
import org.w3c.dom.Document
import java.nio.charset.Charset
import java.util.function.Supplier
import java.util.regex.Pattern
import javax.xml.transform.TransformerException

@Suppress("TooManyFunctions")
open class PactDslResponse @JvmOverloads constructor(
  private val consumerPactBuilder: ConsumerPactBuilder,
  private val request: PactDslRequestWithPath?,
  private val defaultRequestValues: PactDslRequestWithoutPath? = null,
  private val defaultResponseValues: PactDslResponse? = null,
  private val comments: MutableList<String> = mutableListOf(),
  val version: PactSpecVersion = PactSpecVersion.V3
) {
  private var responseStatus = 200
  private val responseHeaders: MutableMap<String, List<String>> = HashMap()
  private var responseBody = missing()
  private var responseMatchers: MatchingRules = MatchingRulesImpl()
  private var responseGenerators = Generators()

  init {
    setupDefaultValues()
  }

  private fun setupDefaultValues() {
    if (defaultResponseValues != null) {
      responseStatus = defaultResponseValues.responseStatus
      responseHeaders.putAll(defaultResponseValues.responseHeaders)
      responseBody = defaultResponseValues.responseBody
      responseMatchers = (defaultResponseValues.responseMatchers as MatchingRulesImpl).copy()
      responseGenerators = Generators(defaultResponseValues.responseGenerators.categories)
    }
  }

  /**
   * Response status code
   *
   * @param status HTTP status code
   */
  fun status(status: Int): PactDslResponse {
    responseStatus = status
    return this
  }

  /**
   * Response headers to return
   *
   * Provide the headers you want to validate, other headers will be ignored.
   *
   * @param headers key-value pairs of headers
   */
  fun headers(headers: Map<String, String>): PactDslResponse {
    for ((key, value) in headers) {
      responseHeaders[key] = listOf(value)
    }
    return this
  }

  /**
   * Response body to return
   *
   * @param body Response body in string form
   */
  fun body(body: String): PactDslResponse {
    responseBody = body(body.toByteArray())
    return this
  }

  /**
   * Response body to return
   *
   * @param body body in string form
   * @param contentType the Content-Type response header value
   */
  fun body(body: String, contentType: String): PactDslResponse {
    return body(body, ContentType.parse(contentType))
  }

  /**
   * Response body to return
   *
   * @param body body in string form
   * @param contentType the Content-Type response header value
   */
  fun body(body: String, contentType: ContentType): PactDslResponse {
    val charset = if (contentType.charset == null) Charset.defaultCharset() else contentType.charset
    responseBody = body(body.toByteArray(charset), au.com.dius.pact.core.model.ContentType(contentType.toString()))
    responseHeaders[CONTENT_TYPE] = listOf(contentType.toString())
    return this
  }

  /**
   * The body of the request
   *
   * @param body Response body in Java Functional Interface Supplier that must return a string
   */
  fun body(body: Supplier<String>): PactDslResponse {
    responseBody = body(body.get().toByteArray())
    return this
  }

  /**
   * The body of the request
   *
   * @param body Response body in Java Functional Interface Supplier that must return a string
   * @param contentType the Content-Type response header value
   */
  fun body(body: Supplier<String>, contentType: String): PactDslResponse {
    return body(body, contentType)
  }

  /**
   * The body of the request
   *
   * @param body Response body in Java Functional Interface Supplier that must return a string
   * @param contentType the Content-Type response header value
   */
  fun body(body: Supplier<String>, contentType: ContentType): PactDslResponse {
    val charset = if (contentType.charset == null) Charset.defaultCharset() else contentType.charset
    responseBody = body(body.get().toByteArray(charset),
      au.com.dius.pact.core.model.ContentType(contentType.toString()))
    responseHeaders[CONTENT_TYPE] = listOf(contentType.toString())
    return this
  }

  /**
   * The body of the request with possible single quotes as delimiters
   * and using [QuoteUtil] to convert single quotes to double quotes if required.
   *
   * @param body Request body in string form
   */
  fun bodyWithSingleQuotes(body: String): PactDslResponse {
    return body(QuoteUtil.convert(body))
  }

  /**
   * The body of the request with possible single quotes as delimiters
   * and using [QuoteUtil] to convert single quotes to double quotes if required.
   *
   * @param body Request body in string form
   * @param contentType the Content-Type response header value
   */
  fun bodyWithSingleQuotes(body: String, contentType: String): PactDslResponse {
    return body(QuoteUtil.convert(body), contentType)
  }

  /**
   * The body of the request with possible single quotes as delimiters
   * and using [QuoteUtil] to convert single quotes to double quotes if required.
   *
   * @param body Request body in string form
   * @param contentType the Content-Type response header value
   */
  fun bodyWithSingleQuotes(body: String, contentType: ContentType): PactDslResponse {
    return body(QuoteUtil.convert(body), contentType)
  }

  /**
   * Response body to return
   *
   * @param body Response body in JSON form
   */
  fun body(body: JSONObject): PactDslResponse {
    responseBody = if (isContentTypeHeaderNotSet) {
      matchHeader(CONTENT_TYPE, DEFAULT_JSON_CONTENT_TYPE_REGEX, ContentType.APPLICATION_JSON.toString())
      body(body.toString().toByteArray())
    } else {
      val contentType = contentTypeHeader
      val ct = ContentType.parse(contentType)
      val charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
      body(body.toString().toByteArray(charset),
        au.com.dius.pact.core.model.ContentType(contentType))
    }
    return this
  }

  /**
   * Response body to return
   *
   * @param body Response body built using the Pact body DSL
   */
  fun body(body: DslPart): PactDslResponse {
    val parent = body.close()!!
    responseMatchers.addCategory(parent.matchers)
    responseGenerators.addGenerators(parent.generators)
    var charset = Charset.defaultCharset()
    var contentType = ContentType.APPLICATION_JSON.toString()
    if (isContentTypeHeaderNotSet) {
      matchHeader(CONTENT_TYPE, DEFAULT_JSON_CONTENT_TYPE_REGEX, contentType)
    } else {
      contentType = contentTypeHeader
      val ct = ContentType.parse(contentType)
      charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
    }
    responseBody = body(parent.body.serialise().toByteArray(charset),
      au.com.dius.pact.core.model.ContentType(contentType))
    return this
  }

  /**
   * Response body to return
   *
   * @param body Response body as an XML Document
   */
  @Throws(TransformerException::class)
  fun body(body: Document): PactDslResponse {
    if (isContentTypeHeaderNotSet) {
      responseHeaders[CONTENT_TYPE] = listOf(ContentType.APPLICATION_XML.toString())
      responseBody = body(ConsumerPactBuilder.xmlToString(body).toByteArray())
    } else {
      val contentType = contentTypeHeader
      val ct = ContentType.parse(contentType)
      val charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
      responseBody = body(ConsumerPactBuilder.xmlToString(body).toByteArray(charset),
        au.com.dius.pact.core.model.ContentType(contentType))
    }
    return this
  }

  /**
   * Response body as a binary data. It will match any expected bodies against the content type.
   * @param example Example contents to use in the consumer test
   * @param contentType Content type of the data
   */
  fun withBinaryData(example: ByteArray, contentType: String): PactDslResponse {
    responseBody = body(example, fromString(contentType))
    responseHeaders[CONTENT_TYPE] = listOf(contentType)
    responseMatchers.addCategory("body").addRule("$", ContentTypeMatcher(contentType))
    return this
  }

  /**
   * Match a response header. A random example header value will be generated from the provided regular
   * expression if the example value is not provided.
   *
   * @param header        Header to match
   * @param regexp        Regular expression to match
   * @param headerExample Example value to use
   */
  @JvmOverloads
  fun matchHeader(header: String, regexp: String?, headerExample: String = Generex(regexp).random()): PactDslResponse {
    responseMatchers.addCategory("header").setRule(header, RegexMatcher(regexp!!))
    responseHeaders[header] = listOf(headerExample)
    return this
  }

  private fun addInteraction() {
    if (version == PactSpecVersion.V4) {
      consumerPactBuilder.interactions.add(V4Interaction.SynchronousHttp(
        "",
        request!!.description,
        request.state,
        HttpRequest(request.requestMethod, request.path, request.query,
          request.requestHeaders, request.requestBody, request.requestMatchers, request.requestGenerators),
        HttpResponse(responseStatus, responseHeaders, responseBody, responseMatchers, responseGenerators),
        null, mutableMapOf("text" to jsonArray(comments))).withGeneratedKey())
    } else {
      consumerPactBuilder.interactions.add(RequestResponseInteraction(
        request!!.description,
        request.state,
        Request(request.requestMethod, request.path, request.query,
          request.requestHeaders, request.requestBody, request.requestMatchers, request.requestGenerators),
        Response(responseStatus, responseHeaders, responseBody, responseMatchers, responseGenerators),
        null
      ))
    }
  }

  /**
   * Terminates the DSL and builds a pact to represent the interactions
   */
  fun <P : BasePact> toPact(pactClass: Class<P>): P {
    addInteraction()
    return when {
      pactClass.isAssignableFrom(V4Pact::class.java) -> {
        V4Pact(request!!.consumer, request.provider,
          consumerPactBuilder.interactions.map { obj -> obj.asV4Interaction() }.toMutableList(), DEFAULT_METADATA,
          UnknownPactSource) as P
      }
      pactClass.isAssignableFrom(RequestResponsePact::class.java) -> {
        RequestResponsePact(request!!.provider, request.consumer,
          consumerPactBuilder.interactions.map { it.asSynchronousRequestResponse()!! }.toMutableList()) as P
      }
      else -> throw IllegalArgumentException(pactClass.simpleName + " is not a valid Pact class")
    }
  }

  /**
   * Terminates the DSL and builds a pact to represent the interactions
   */
  fun toPact(): RequestResponsePact {
    return toPact(RequestResponsePact::class.java)
  }

  /**
   * Description of the request that is expected to be received
   *
   * @param description request description
   */
  fun uponReceiving(description: String): PactDslRequestWithPath {
    addInteraction()
    return PactDslRequestWithPath(consumerPactBuilder, request!!, description, defaultRequestValues,
      defaultResponseValues, comments, version)
  }

  /**
   * Adds a provider state to this interaction
   * @param state Description of the state
   */
  fun given(state: String): PactDslWithState {
    addInteraction()
    return PactDslWithState(consumerPactBuilder, request!!.consumer.name, request.provider.name,
      ProviderState(state), defaultRequestValues, defaultResponseValues, version)
  }

  /**
   * Adds a provider state to this interaction
   * @param state Description of the state
   * @param params Data parameters for this state
   */
  fun given(state: String, params: Map<String, Any>): PactDslWithState {
    addInteraction()
    return PactDslWithState(consumerPactBuilder, request!!.consumer.name, request.provider.name,
      ProviderState(state, params), defaultRequestValues, defaultResponseValues, version)
  }

  /**
   * Adds a header that will have it's value injected from the provider state
   * @param name Header Name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  fun headerFromProviderState(name: String, expression: String, example: String): PactDslResponse {
    responseGenerators.addGenerator(Category.HEADER, name, ProviderStateGenerator(expression, DataType.STRING))
    responseHeaders[name] = listOf(example)
    return this
  }

  /**
   * Match a set cookie header
   * @param cookie Cookie name to match
   * @param regex Regex to match the cookie value with
   * @param example Example value
   */
  fun matchSetCookie(cookie: String, regex: String, example: String): PactDslResponse {
    val header = responseMatchers.addCategory("header")
    if (header.numRules("set-cookie") > 0) {
      header.addRule("set-cookie", RegexMatcher(Pattern.quote("$cookie=") + regex))
    } else {
      header.setRule("set-cookie", RegexMatcher(Pattern.quote("$cookie=") + regex), RuleLogic.OR)
    }
    if (responseHeaders.containsKey("set-cookie")) {
      responseHeaders["set-cookie"] = responseHeaders["set-cookie"]!!.plus("$cookie=$example")
    } else {
      responseHeaders["set-cookie"] = listOf("$cookie=$example")
    }
    return this
  }

  /**
   * XML Response body to return
   *
   * @param xmlBuilder XML Builder used to construct the XML document
   */
  fun body(xmlBuilder: PactXmlBuilder): PactDslResponse {
    responseMatchers.addCategory(xmlBuilder.matchingRules)
    responseGenerators.addGenerators(xmlBuilder.generators)
    if (isContentTypeHeaderNotSet) {
      responseHeaders[CONTENT_TYPE] = listOf(ContentType.APPLICATION_XML.toString())
      responseBody = body(xmlBuilder.asBytes())
    } else {
      val contentType = contentTypeHeader
      val ct = ContentType.parse(contentType)
      val charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
      responseBody = body(xmlBuilder.asBytes(charset),
        au.com.dius.pact.core.model.ContentType(contentType))
    }
    return this
  }

  /**
   * Adds a comment to this interaction
   */
  fun comment(comment: String): PactDslResponse {
    this.comments.add(comment)
    return this
  }

  protected val isContentTypeHeaderNotSet: Boolean
    get() = responseHeaders.keys.none { key -> key.equals(CONTENT_TYPE, ignoreCase = true) }
  protected val contentTypeHeader: String
    get() = responseHeaders.entries.find { (key, _) -> key.equals(CONTENT_TYPE, ignoreCase = true) }
      ?.value?.get(0) ?: ""

  companion object {
    private const val CONTENT_TYPE = "Content-Type"
    const val DEFAULT_JSON_CONTENT_TYPE_REGEX = "application/json(;\\s?charset=[\\w\\-]+)?"
  }
}
