package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.HttpResponse
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

class HttpResponseBuilderSpec extends Specification {

  HttpResponseBuilder builder

  def setup() {
    builder = new HttpResponseBuilder(new HttpResponse())
  }

  def 'with defaults'() {
    expect:
    builder.build() == new HttpResponse()
  }

  def 'allows adding headers to the response'() {
    when:
    def response = builder
      .header('A', 'B')
      .header('B', ['B', 'C', 'D'])
      .header('OPTIONS', 'GET, POST, HEAD')
      .headers([x: 'y', y: ['a', 'b', 'c']])
      .headers('x1', 'y', 'y1', 'a', 'y1', 'b', 'y1', 'c')
      .headers(new Pair('x2', 'y'), new Pair('y2', 'a'), new Pair('y2', 'b'), new Pair('y2', 'c'))
      .build()

    then:
    response.headers == [
      A: ['B'],
      B: ['B', 'C', 'D'],
      OPTIONS: ['GET', 'POST', 'HEAD'],
      x: ['y'],
      y: ['a', 'b', 'c'],
      x1: ['y'],
      y1: ['a', 'b', 'c'],
      x2: ['y'],
      y2: ['a', 'b', 'c']
    ]
  }

  def 'allows using matching rules with headers'() {
    when:
    def response = builder
      .header('A', regexp('\\d+', '111'))
      .header('B', ['B', numeric(100), 'D'])
      .headers([x: regexp('\\d+', '111'), y: ['a', regexp('\\d+', '111'), 'c']])
      .headers(new Pair('x2', regexp('\\d+', '111')), new Pair('y2', 'a'))
      .build()

    then:
    response.headers == [
      'A': ['111'],
      'B': ['B', '100', 'D'],
      'x': ['111'],
      'y': ['a', '111', 'c'],
      x2: ['111'],
      y2: ['a']
    ]
    response.matchingRules.rulesForCategory('header') == new MatchingRuleCategory('header',
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
    def response = builder
      .header('A', fromProviderState('$a', '111'))
      .build()

    then:
    response.headers == [
      'A': ['111']
    ]
    response.matchingRules.rulesForCategory('header') == new MatchingRuleCategory('header', [:])
    response.generators.categoryFor(Category.HEADER) == [A: new ProviderStateGenerator('$a')]
  }

  def 'allows setting the response status'() {
    when:
    def response = builder
      .status(204)
      .build()

    then:
    response.status == 204
  }

  def 'allows setting the body of the response as a string value'() {
    when:
    def response = builder
      .body('This is some text')
      .build()

    then:
    response.body.valueAsString() == 'This is some text'
    response.body.contentType.toString() == 'text/plain; charset=ISO-8859-1'
    response.headers['content-type'] == ['text/plain; charset=ISO-8859-1']
  }

  def 'allows setting the body of the response as a string value with a given content type'() {
    when:
    def response = builder
      .body('This is some text', 'text/test-special')
      .build()

    then:
    response.body.valueAsString() == 'This is some text'
    response.body.contentType.toString() == 'text/test-special'
    response.headers['content-type'] == ['text/test-special']
  }

  def 'when setting the body, tries to detect the content type from the body contents'() {
    when:
    def response = builder
      .body('{"value": "This is some text"}')
      .build()

    then:
    response.body.valueAsString() == '{"value": "This is some text"}'
    response.body.contentType.toString() == 'application/json'
    response.headers['content-type'] == ['application/json']
  }

  def 'when setting the body, uses any existing content type header'() {
    when:
    def response = builder
      .header('content-type', 'text/plain')
      .body('{"value": "This is some text"}')
      .build()

    then:
    response.body.valueAsString() == '{"value": "This is some text"}'
    response.body.contentType.toString() == 'text/plain'
    response.headers['content-type'] == ['text/plain']
  }

  def 'when setting the body, overrides any existing content type header if the content type is given'() {
    when:
    def response = builder
      .header('content-type', 'text/plain')
      .body('{"value": "This is some text"}', 'application/json')
      .build()

    then:
    response.body.valueAsString() == '{"value": "This is some text"}'
    response.body.contentType.toString() == 'application/json'
    response.headers['content-type'] == ['application/json']
  }

  // /**
  //   * Match a set cookie header
  //   * @param cookie Cookie name to match
  //   * @param regex Regex to match the cookie value with
  //   * @param example Example value
  //   */
  //  fun matchSetCookie(cookie: String, regex: String, example: String): PactDslResponse {
  //    val header = responseMatchers.addCategory("header")
  //    if (header.numRules("set-cookie") > 0) {
  //      header.addRule("set-cookie", RegexMatcher(Pattern.quote("$cookie=") + regex))
  //    } else {
  //      header.setRule("set-cookie", RegexMatcher(Pattern.quote("$cookie=") + regex), RuleLogic.OR)
  //    }
  //    if (responseHeaders.containsKey("set-cookie")) {
  //      responseHeaders["set-cookie"] = responseHeaders["set-cookie"]!!.plus("$cookie=$example")
  //    } else {
  //      responseHeaders["set-cookie"] = listOf("$cookie=$example")
  //    }
  //    return this
  //  }

  // /**
  //   * Match any HTTP Information response status (100-199)
  //   */
  //  fun informationStatus(): PactDslResponse {
  //    val matcher = StatusCodeMatcher(HttpStatus.Information)
  //    responseMatchers.addCategory("status").addRule(matcher)
  //    responseStatus = 100
  //    return this
  //  }
  //
  //  /**
  //   * Match any HTTP success response status (200-299)
  //   */
  //  fun successStatus(): PactDslResponse {
  //    val matcher = StatusCodeMatcher(HttpStatus.Success)
  //    responseMatchers.addCategory("status").addRule(matcher)
  //    responseStatus = 200
  //    return this
  //  }
  //
  //  /**
  //   * Match any HTTP redirect response status (300-399)
  //   */
  //  fun redirectStatus(): PactDslResponse {
  //    val matcher = StatusCodeMatcher(HttpStatus.Redirect)
  //    responseMatchers.addCategory("status").addRule(matcher)
  //    responseStatus = 300
  //    return this
  //  }
  //
  //  /**
  //   * Match any HTTP client error response status (400-499)
  //   */
  //  fun clientErrorStatus(): PactDslResponse {
  //    val matcher = StatusCodeMatcher(HttpStatus.ClientError)
  //    responseMatchers.addCategory("status").addRule(matcher)
  //    responseStatus = 400
  //    return this
  //  }
  //
  //  /**
  //   * Match any HTTP server error response status (500-599)
  //   */
  //  fun serverErrorStatus(): PactDslResponse {
  //    val matcher = StatusCodeMatcher(HttpStatus.ServerError)
  //    responseMatchers.addCategory("status").addRule(matcher)
  //    responseStatus = 500
  //    return this
  //  }
  //
  //  /**
  //   * Match any HTTP non-error response status (< 400)
  //   */
  //  fun nonErrorStatus(): PactDslResponse {
  //    val matcher = StatusCodeMatcher(HttpStatus.NonError)
  //    responseMatchers.addCategory("status").addRule(matcher)
  //    responseStatus = 200
  //    return this
  //  }
  //
  //  /**
  //   * Match any HTTP error response status (>= 400)
  //   */
  //  fun errorStatus(): PactDslResponse {
  //    val matcher = StatusCodeMatcher(HttpStatus.Error)
  //    responseMatchers.addCategory("status").addRule(matcher)
  //    responseStatus = 400
  //    return this
  //  }
  //
  //  /**
  //   * Match any HTTP status code in the provided list
  //   */
  //  fun statusCodes(statusCodes: List<Int>): PactDslResponse {
  //    val matcher = StatusCodeMatcher(HttpStatus.StatusCodes, statusCodes)
  //    responseMatchers.addCategory("status").addRule(matcher)
  //    responseStatus = statusCodes.first()
  //    return this
  //  }
}
