package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import kotlin.Pair
import spock.lang.Specification

import static au.com.dius.pact.consumer.dsl.Matchers.fromProviderState
import static au.com.dius.pact.consumer.dsl.Matchers.numeric
import static au.com.dius.pact.consumer.dsl.Matchers.regexp

class HttpRequestBuilderSpec extends Specification {

  HttpRequestBuilder builder

  def setup() {
    builder = new HttpRequestBuilder(new HttpRequest())
  }

  def 'with defaults'() {
    expect:
    builder.build() == new HttpRequest()
  }

  def 'allows setting the request method'() {
    when:
    def request = builder.method('AARGH').build()

    then:
    request.method == 'AARGH'
  }

  def 'allows setting the request path'() {
    when:
    def request = builder.path('/path').build()

    then:
    request.path == '/path'
  }

  def 'allows using a matcher with the request path'() {
    when:
    def request = builder.path(regexp('\\/path\\/\\d+', "/path/1000")).build()

    then:
    request.path == '/path/1000'
    request.matchingRules.rulesForCategory('path') == new MatchingRuleCategory('path',
      ['': new MatchingRuleGroup([new RegexMatcher('\\/path\\/\\d+', '/path/1000')])]
    )
  }

  def 'allows adding headers to the request'() {
    when:
    def request = builder
      .header('A', 'B')
      .header('B', ['B', 'C', 'D'])
      .header('OPTIONS', 'GET, POST, HEAD')
      .header('content-type', 'application/x;charset=UTF-8')
      .header('date', 'Fri, 13 Jan 2023 04:39:16 GMT')
      .headers([x: 'y', y: ['a', 'b', 'c']])
      .headers('x1', 'y', 'y1', 'a', 'y1', 'b', 'y1', 'c')
      .headers(new Pair('x2', 'y'), new Pair('y2', 'a'), new Pair('y2', 'b'), new Pair('y2', 'c'))
      .build()

    then:
    request.headers == [
      'A': ['B'],
      'B': ['B', 'C', 'D'],
      'OPTIONS': ['GET', 'POST', 'HEAD'],
      'content-type': ['application/x;charset=UTF-8'],
      'date': ['Fri, 13 Jan 2023 04:39:16 GMT'],
      'x': ['y'],
      'y': ['a', 'b', 'c'],
      'x1': ['y'],
      'y1': ['a', 'b', 'c'],
      x2: ['y'],
      y2: ['a', 'b', 'c']
    ]
  }

  def 'allows using matching rules with headers'() {
    when:
    def request = builder
      .header('A', regexp('\\d+', '111'))
      .header('B', ['B', numeric(100), 'D'])
      .headers([x: regexp('\\d+', '111'), y: ['a', regexp('\\d+', '111'), 'c']])
      .headers(new Pair('x2', regexp('\\d+', '111')), new Pair('y2', 'a'))
      .build()

    then:
    request.headers == [
      'A': ['111'],
      'B': ['B', '100', 'D'],
      'x': ['111'],
      'y': ['a', '111', 'c'],
      x2: ['111'],
      y2: ['a']
    ]
    request.matchingRules.rulesForCategory('header') == new MatchingRuleCategory('header',
      [
        A: new MatchingRuleGroup([new RegexMatcher('\\d+', '111')]),
        'B[1]': new MatchingRuleGroup([new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)]),
        x: new MatchingRuleGroup([new RegexMatcher('\\d+', '111')]),
        'y[1]': new MatchingRuleGroup([new RegexMatcher('\\d+', '111')]),
        x2: new MatchingRuleGroup([new RegexMatcher('\\d+', '111')])
      ]
    )
  }

  def 'supports setting header values from provider states'() {
    when:
    def request = builder
      .header('A', fromProviderState('$a', '111'))
      .build()

    then:
    request.headers == [
      'A': ['111']
    ]
    request.matchingRules.rulesForCategory('header') == new MatchingRuleCategory('header', [:])
    request.generators.categoryFor(Category.HEADER) == [A: new ProviderStateGenerator('$a')]
  }

  def 'allows setting the body of the request as a string value'() {
    when:
    def request = builder
      .body('This is some text')
      .build()

    then:
    request.body.valueAsString() == 'This is some text'
    request.body.contentType.toString() == 'text/plain; charset=ISO-8859-1'
    request.headers['content-type'] == ['text/plain; charset=ISO-8859-1']
  }

  def 'allows setting the body of the request as a string value with a given content type'() {
    when:
    def request = builder
      .body('This is some text', 'text/test-special')
      .build()

    then:
    request.body.valueAsString() == 'This is some text'
    request.body.contentType.toString() == 'text/test-special'
    request.headers['content-type'] == ['text/test-special']
  }

  def 'when setting the body, tries to detect the content type from the body contents'() {
    when:
    def request = builder
      .body('{"value": "This is some text"}')
      .build()

    then:
    request.body.valueAsString() == '{"value": "This is some text"}'
    request.body.contentType.toString() == 'application/json'
    request.headers['content-type'] == ['application/json']
  }

  def 'when setting the body, uses any existing content type header'() {
    when:
    def request = builder
      .header('content-type', 'text/plain')
      .body('{"value": "This is some text"}')
      .build()

    then:
    request.body.valueAsString() == '{"value": "This is some text"}'
    request.body.contentType.toString() == 'text/plain'
    request.headers['content-type'] == ['text/plain']
  }

  def 'when setting the body, overrides any existing content type header if the content type is given'() {
    when:
    def request = builder
      .header('content-type', 'text/plain')
      .body('{"value": "This is some text"}', 'application/json')
      .build()

    then:
    request.body.valueAsString() == '{"value": "This is some text"}'
    request.body.contentType.toString() == 'application/json'
    request.headers['content-type'] == ['application/json']
  }

  //  /**
  //   * The query string for the request
  //   *
  //   * @param query query string
  //   */
  //  fun query(query: String): PactDslRequestWithoutPath {
  //    this.query = queryStringToMap(query, false).toMutableMap()
  //    return this
  //  }
  //
  //  /**
  //   * The body of the request
  //   *
  //   * @param body Request body in string form
  //   */
  //  fun body(body: String, contentType: ContentType): PactDslRequestWithoutPath {
  //    val charset = if (contentType.charset == null) Charset.defaultCharset() else contentType.charset
  //    requestBody = body(body.toByteArray(charset), au.com.dius.pact.core.model.ContentType(contentType.toString()))
  //    requestHeaders[CONTENT_TYPE] = listOf(contentType.toString())
  //    return this
  //  }
  //
  //  /**
  //   * The body of the request
  //   *
  //   * @param body Request body in Java Functional Interface Supplier that must return a string
  //   */
  //  fun body(body: Supplier<String>): PactDslRequestWithoutPath {
  //    requestBody = body(body.get().toByteArray())
  //    return this
  //  }
  //
  //  /**
  //   * The body of the request
  //   *
  //   * @param body Request body in Java Functional Interface Supplier that must return a string
  //   */
  //  fun body(body: Supplier<String>, contentType: String): PactDslRequestWithoutPath {
  //    return this.body(body, ContentType.parse(contentType))
  //  }
  //
  //  /**
  //   * The body of the request
  //   *
  //   * @param body Request body in Java Functional Interface Supplier that must return a string
  //   */
  //  fun body(body: Supplier<String>, contentType: ContentType): PactDslRequestWithoutPath {
  //    val charset = if (contentType.charset == null) Charset.defaultCharset() else contentType.charset
  //    requestBody = body(body.get().toByteArray(charset), au.com.dius.pact.core.model.ContentType(
  //    contentType.toString()))
  //    requestHeaders[CONTENT_TYPE] = listOf(contentType.toString())
  //    return this
  //  }
  //
  //  /**
  //   * The body of the request with possible single quotes as delimiters
  //   * and using [QuoteUtil] to convert single quotes to double quotes if required.
  //   *
  //   * @param body Request body in string form
  //   */
  //  fun bodyWithSingleQuotes(body: String): PactDslRequestWithoutPath {
  //    return body(QuoteUtil.convert(body))
  //  }
  //
  //  /**
  //   * The body of the request with possible single quotes as delimiters
  //   * and using [QuoteUtil] to convert single quotes to double quotes if required.
  //   *
  //   * @param body Request body in string form
  //   */
  //  fun bodyWithSingleQuotes(body: String, contentType: String): PactDslRequestWithoutPath {
  //    return body(QuoteUtil.convert(body), contentType)
  //  }
  //
  //  /**
  //   * The body of the request with possible single quotes as delimiters
  //   * and using [QuoteUtil] to convert single quotes to double quotes if required.
  //   *
  //   * @param body Request body in string form
  //   */
  //  fun bodyWithSingleQuotes(body: String, contentType: ContentType): PactDslRequestWithoutPath {
  //    return body(QuoteUtil.convert(body), contentType)
  //  }
  //
  //  /**
  //   * The body of the request
  //   *
  //   * @param body Request body in JSON form
  //   */
  //  fun body(body: JSONObject): PactDslRequestWithoutPath {
  //    if (isContentTypeHeaderNotSet) {
  //      requestHeaders[CONTENT_TYPE] = listOf(ContentType.APPLICATION_JSON.toString())
  //      requestBody = body(body.toString().toByteArray())
  //    } else {
  //      val contentType = contentTypeHeader
  //      val ct = ContentType.parse(contentType)
  //      val charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
  //      requestBody = body(body.toString().toByteArray(charset),
  //        au.com.dius.pact.core.model.ContentType(contentType))
  //    }
  //    return this
  //  }
  //
  //  /**
  //   * The body of the request
  //   *
  //   * @param body Built using the Pact body DSL
  //   */
  //  fun body(body: DslPart): PactDslRequestWithoutPath {
  //    val parent = body.close()
  //
  //    requestMatchers.addCategory(parent!!.matchers)
  //    requestGenerators.addGenerators(parent.generators)
  //
  //    if (isContentTypeHeaderNotSet) {
  //      requestHeaders[CONTENT_TYPE] = listOf(ContentType.APPLICATION_JSON.toString())
  //      requestBody = body(parent.toString().toByteArray())
  //    } else {
  //      val contentType = contentTypeHeader
  //      val ct = ContentType.parse(contentType)
  //      val charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
  //      requestBody = body(parent.toString().toByteArray(charset),
  //        au.com.dius.pact.core.model.ContentType(contentType))
  //    }
  //    return this
  //  }
  //
  //  /**
  //   * The body of the request
  //   *
  //   * @param body XML Document
  //   */
  //  @Throws(TransformerException::class)
  //  fun body(body: Document): PactDslRequestWithoutPath {
  //    if (isContentTypeHeaderNotSet) {
  //      requestHeaders[CONTENT_TYPE] = listOf(ContentType.APPLICATION_XML.toString())
  //      requestBody = body(ConsumerPactBuilder.xmlToString(body).toByteArray())
  //    } else {
  //      val contentType = contentTypeHeader
  //      val ct = ContentType.parse(contentType)
  //      val charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
  //      requestBody = body(ConsumerPactBuilder.xmlToString(body).toByteArray(charset),
  //        au.com.dius.pact.core.model.ContentType(contentType))
  //    }
  //    return this
  //  }
  //
  //  /**
  //   * XML Response body to return
  //   *
  //   * @param xmlBuilder XML Builder used to construct the XML document
  //   */
  //  fun body(xmlBuilder: PactXmlBuilder): PactDslRequestWithoutPath {
  //    requestMatchers.addCategory(xmlBuilder.matchingRules)
  //    requestGenerators.addGenerators(xmlBuilder.generators)
  //    if (isContentTypeHeaderNotSet) {
  //      requestHeaders[CONTENT_TYPE] = listOf(ContentType.APPLICATION_XML.toString())
  //      requestBody = body(xmlBuilder.asBytes())
  //    } else {
  //      val contentType = contentTypeHeader
  //      val ct = ContentType.parse(contentType)
  //      val charset = if (ct.charset != null) ct.charset else Charset.defaultCharset()
  //      requestBody = body(xmlBuilder.asBytes(charset),
  //        au.com.dius.pact.core.model.ContentType(contentType))
  //    }
  //    return this
  //  }
  //
  //  /**
  //   * The body of the request
  //   *
  //   * @param body Built using MultipartEntityBuilder
  //   */
  //  open fun body(body: MultipartEntityBuilder): PactDslRequestWithoutPath {
  //    setupMultipart(body)
  //    return this
  //  }
  //
  //  /**
  //   * Sets up a content type matcher to match any body of the given content type
  //   */
  //  public override fun bodyMatchingContentType(contentType: String, exampleContents: String)
  //    return super.bodyMatchingContentType(contentType, exampleContents) as PactDslRequestWithoutPath
  //  }
  //  /**
  //   * Sets up a file upload request. This will add the correct content type header to the request
  //   * @param partName This is the name of the part in the multipart body.
  //   * @param fileName This is the name of the file that was uploaded
  //   * @param fileContentType This is the content type of the uploaded file
  //   * @param data This is the actual file contents
  //   */
  //  @Throws(IOException::class)
  //  fun withFileUpload(
  //    partName: String,
  //    fileName: String,
  //    fileContentType: String?,
  //    data: ByteArray
  //  ): PactDslRequestWithoutPath {
  //    setupFileUpload(partName, fileName, fileContentType, data)
  //    return this
  //  }
  //
  //  /**
  //   * Matches a date field using the provided date pattern
  //   * @param field field name
  //   * @param pattern pattern to match
  //   * @param example Example value
  //   */
  //  fun queryMatchingDate(field: String, pattern: String, example: String): PactDslRequestWithoutPath {
  //    return queryMatchingDateBase(field, pattern, example) as PactDslRequestWithoutPath
  //  }
  //
  //  /**
  //   * Matches a date field using the provided date pattern. The current system date will be used for the
  //   example value.
  //   * @param field field name
  //   * @param pattern pattern to match
  //   */
  //  fun queryMatchingDate(field: String, pattern: String): PactDslRequestWithoutPath {
  //    return queryMatchingDateBase(field, pattern, null) as PactDslRequestWithoutPath
  //  }
  //
  //  /**
  //   * Matches a time field using the provided time pattern
  //   * @param field field name
  //   * @param pattern pattern to match
  //   * @param example Example value
  //   */
  //  fun queryMatchingTime(field: String, pattern: String, example: String): PactDslRequestWithoutPath {
  //    return queryMatchingTimeBase(field, pattern, example) as PactDslRequestWithoutPath
  //  }
  //
  //  /**
  //   * Matches a time field using the provided time pattern. The current system time will be used for the
  //   example value.
  //   * @param field field name
  //   * @param pattern pattern to match
  //   */
  //  fun queryMatchingTime(field: String, pattern: String): PactDslRequestWithoutPath {
  //    return queryMatchingTimeBase(field, pattern, null) as PactDslRequestWithoutPath
  //  }
  //
  //  /**
  //   * Matches a datetime field using the provided pattern
  //   * @param field field name
  //   * @param pattern pattern to match
  //   * @param example Example value
  //   */
  //  fun queryMatchingDatetime(field: String, pattern: String, example: String): PactDslRequestWithoutPath {
  //    return queryMatchingDatetimeBase(field, pattern, example) as PactDslRequestWithoutPath
  //  }
  //
  //  /**
  //   * Matches a datetime field using the provided pattern. The current system date and time will be used for the
  //   * example value.
  //   * @param field field name
  //   * @param pattern pattern to match
  //   */
  //  fun queryMatchingDatetime(field: String, pattern: String): PactDslRequestWithoutPath {
  //    return queryMatchingDatetimeBase(field, pattern, null) as PactDslRequestWithoutPath
  //  }
  //
  //  /**
  //   * Matches a date field using the ISO date pattern.  The current system date will be used for the example value
  //   * if not provided.
  //   * @param field field name
  //   * @param example Example value
  //   */
  //  @JvmOverloads
  //  fun queryMatchingISODate(field: String, example: String? = null): PactDslRequestWithoutPath {
  //    return queryMatchingDateBase(field, DateFormatUtils.ISO_DATE_FORMAT.pattern, example)
  //  }
  //
  //  /**
  //   * Matches a time field using the ISO time pattern
  //   * @param field field name
  //   * @param example Example value
  //   */
  //  fun queryMatchingISOTime(field: String, example: String?): PactDslRequestWithoutPath {
  //    return queryMatchingTimeBase(field, DateFormatUtils.ISO_TIME_FORMAT.pattern, example)
  //  }
  //
  //  /**
  //   * Matches a time field using the ISO time pattern. The current system time will be used for the example value.
  //   * @param field field name
  //   */
  //  fun queryMatchingTime(field: String): PactDslRequestWithoutPath {
  //    return queryMatchingISOTime(field, null)
  //  }
  //
  //  /**
  //   * Matches a datetime field using the ISO pattern. The current system date and time will be used for the example
  //   * value if not provided.
  //   * @param field field name
  //   * @param example Example value
  //   */
  //  @JvmOverloads
  //  fun queryMatchingISODatetime(field: String, example: String? = null): PactDslRequestWithoutPath {
  //    return queryMatchingDatetimeBase(field, DateFormatUtils.ISO_DATETIME_FORMAT.pattern,
  //      example) as PactDslRequestWithoutPath
  //  }
}
