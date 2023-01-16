package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import kotlin.Pair
import spock.lang.Specification

import static au.com.dius.pact.consumer.dsl.Matchers.date
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
    def request = builder.path(regexp('\\/path\\/\\d+', '/path/1000')).build()

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

  def 'allows adding query parameters to the request'() {
    when:
    def request = builder
      .queryParameter('A', 'B')
      .queryParameter('B', ['B', 'C', 'D'])
      .queryParameters('sx=y&sy=a&sy=b&sy=c')
      .queryParameters([x: 'y', y: ['a', 'b', 'c']])
      .queryParameters('x1', 'y', 'y1', 'a', 'y1', 'b', 'y1', 'c')
      .queryParameters(new Pair('x2', 'y'), new Pair('y2', 'a'), new Pair('y2', 'b'), new Pair('y2', 'c'))
      .build()

    then:
    request.query == [
      'A': ['B'],
      'B': ['B', 'C', 'D'],
      'x': ['y'],
      'y': ['a', 'b', 'c'],
      'sx': ['y'],
      'sy': ['a', 'b', 'c'],
      'x1': ['y'],
      'y1': ['a', 'b', 'c'],
      x2: ['y'],
      y2: ['a', 'b', 'c']
    ]
  }

  def 'allows using matching rules with query parameters'() {
    when:
    def request = builder
      .queryParameter('A', regexp('\\d+', '111'))
      .queryParameter('B', ['B', numeric(100), 'D'])
      .queryParameters([x: date('yyyy', '1111'), y: ['a', date('yyyy'), 'c']])
      .queryParameters(new Pair('x2', regexp('\\d+', '111')), new Pair('y2', 'a'))
      .build()

    then:
    request.query == [
      'A': ['111'],
      'B': ['B', '100', 'D'],
      'x': ['1111'],
      'y': ['a', '2000', 'c'],
      x2: ['111'],
      y2: ['a']
    ]
    request.matchingRules.rulesForCategory('query') == new MatchingRuleCategory('query',
      [
        A: new MatchingRuleGroup([new RegexMatcher('\\d+', '111')]),
        'B[1]': new MatchingRuleGroup([new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)]),
        x: new MatchingRuleGroup([new DateMatcher('yyyy')]),
        'y[1]': new MatchingRuleGroup([new DateMatcher('yyyy')]),
        x2: new MatchingRuleGroup([new RegexMatcher('\\d+', '111')])
      ]
    )
    request.generators.categoryFor(Category.QUERY) == ['y[1]': new DateGenerator('yyyy')]
  }

  def 'supports setting query parameters from provider states'() {
    when:
    def request = builder
      .queryParameter('A', fromProviderState('$a', '111'))
      .build()

    then:
    request.query == [
      'A': ['111']
    ]
    request.matchingRules.rulesForCategory('query') == new MatchingRuleCategory('query', [:])
    request.generators.categoryFor(Category.QUERY) == [A: new ProviderStateGenerator('$a')]
  }

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
}
