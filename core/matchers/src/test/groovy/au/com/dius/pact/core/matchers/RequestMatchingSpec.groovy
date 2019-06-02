package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactReaderKt
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import spock.lang.Specification

class RequestMatchingSpec extends Specification {

  private request, response, interaction, testState

  def setup() {
    request = new Request('GET', '/', PactReaderKt.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: ['testreqheadervalue']],
      OptionalBody.body('{"test": true}'.bytes))

    response = new Response(200, [testreqheader: ['testreqheaderval']],
      OptionalBody.body('{"responsetest": true}'.bytes))

    testState = [new ProviderState('test state')]
  }

  def test(Request actual) {
    interaction = new RequestResponseInteraction('test interaction', testState, request, response)
    new RequestMatching([interaction]).findResponse(actual)
  }

  def 'request matching should match the valid request'() {
    expect:
    test(request) == response
  }

  def 'request matching should disallow additional keys'() {
    given:
    def leakyRequest = request.copy()
    leakyRequest.body = OptionalBody.body('{"test": true, "extra": false}'.bytes)

    when:
    def actualResponse = test(leakyRequest)

    then:
    !actualResponse
  }

  def 'request matching should require precise matching'() {
    given:
    def impreciseRequest = request.copy()
    impreciseRequest.body = OptionalBody.body('{"test": false}'.bytes)

    when:
    def actualResponse = test(impreciseRequest)

    then:
    !actualResponse
  }

  def 'request matching should trim protocol, server name and port'() {
    given:
    def fancyRequest = request.copy()
    fancyRequest.path = 'http://localhost:9090/'

    when:
    def actualResponse = test(fancyRequest)

    then:
    actualResponse == response
  }

  def 'request matching should fail to match when missing headers'() {
    given:
    def headerlessRequest = request.copy()
    headerlessRequest.headers = [:]

    when:
    def actualResponse = test(headerlessRequest)

    then:
    !actualResponse
  }

  def 'request matching should fail to match when headers are present but contain incorrect value'() {
    given:
    def incorrectRequest = request.copy()
    incorrectRequest.headers = [testreqheader: ['incorrectValue']]

    when:
    def actualResponse = test(incorrectRequest)

    then:
    !actualResponse
  }

  def 'request matching should allow additional headers'() {
    given:
    def extraHeaderRequest = request.copy()
    extraHeaderRequest.headers.additonal = ['header']

    when:
    def actualResponse = test(extraHeaderRequest)

    then:
    actualResponse == response
  }

  def 'request matching should allow query string in different order'() {
    given:
    def queryRequest = request.copy()
    queryRequest.query = PactReaderKt.queryStringToMap('r=s&q=p&q=p2')

    when:
    def actualResponse = test(queryRequest)

    then:
    actualResponse == response
  }

  def 'request matching should fail if query string has the same parameter repeated in different order'() {
    given:
    def queryRequest = request.copy()
    queryRequest.query = PactReaderKt.queryStringToMap('r=s&q=p2&q=p')

    when:
    def actualResponse = test(queryRequest)

    then:
    !actualResponse
  }

  def 'request with cookie should match if actual cookie exactly matches the expected'() {
    given:
    request = new Request('GET', '/', [:], [Cookie: ['key1=value1;key2=value2']], OptionalBody.body(''.bytes))
    def cookieRequest = request.copy()
    cookieRequest.headers.Cookie = ['key1=value1;key2=value2']

    when:
    def actualResponse = test(cookieRequest)

    then:
    actualResponse == response
  }

  def 'request with cookie should mismatch if actual cookie contains less data than expected cookie'() {
    given:
    request = new Request('GET', '/', [:], [Cookie: ['key1=value1;key2=value2']], OptionalBody.body(''.bytes))
    def cookieRequest = request.copy()
    cookieRequest.headers.Cookie = ['key2=value2']

    when:
    def actualResponse = test(cookieRequest)

    then:
    !actualResponse
  }

  def 'request with cookie should match if actual cookie contains more data than expected one'() {
    given:
    request = new Request('GET', '/', [:], [Cookie: ['key1=value1;key2=value2']], OptionalBody.body(''.bytes))
    def cookieRequest = request.copy()
    cookieRequest.headers.Cookie = ['key2=value2;key1=value1;key3=value3']

    when:
    def actualResponse = test(cookieRequest)

    then:
    actualResponse == response
  }

  def 'request with cookie should mismatch if actual cookie has no intersection with expected request'() {
    given:
    request = new Request('GET', '/', [:], [Cookie: ['key1=value1;key2=value2']], OptionalBody.body(''.bytes))
    def cookieRequest = request.copy()
    cookieRequest.headers.Cookie = ['key5=value5']

    when:
    def actualResponse = test(cookieRequest)

    then:
    !actualResponse
  }

  def 'request with cookie should match when cookie field is different from cases'() {
    given:
    request = new Request('GET', '/', [:], [Cookie: ['key1=value1;key2=value2']], OptionalBody.body(''.bytes))
    def cookieRequest = request.copy()
    cookieRequest.headers = [cOoKie: ['key1=value1;key2=value2']]

    when:
    def actualResponse = test(cookieRequest)

    then:
    actualResponse == response
  }

  def 'request with cookie should match when there are spaces between cookie items'() {
    given:
    request = new Request('GET', '/', [:], [Cookie: ['key1=value1;key2=value2']], OptionalBody.body(''.bytes))
    def cookieRequest = request.copy()
    cookieRequest.headers.Cookie = ['key1=value1; key2=value2']

    when:
    def actualResponse = test(cookieRequest)

    then:
    actualResponse == response
  }

  def 'path matching should match when the paths are equal'() {
    given:
    request = new Request('GET', '/path')

    when:
    def actualResponse = test(request)

    then:
    actualResponse == response
  }

  def 'path matching should not match when the paths are different'() {
    given:
    request = new Request('GET', '/path')
    def requestWithDifferentPath = request.copy()
    requestWithDifferentPath.path = '/path2'

    when:
    def actualResponse = test(requestWithDifferentPath)

    then:
    !actualResponse
  }

  def 'path matching should allow matching with a defined matcher'() {
    given:
    request = new Request('GET', '/path')
    request.matchingRules.addCategory('path').addRule(new RegexMatcher('/path[0-9]*'))
    def requestWithMatcher = request.copy()
    requestWithMatcher.path = '/path2'

    when:
    def actualResponse = test(requestWithMatcher)

    then:
    actualResponse == response
  }

  def 'path matching should not match with the defined matcher'() {
    given:
    request = new Request('GET', '/path')
    request.matchingRules.addCategory('path').addRule(new RegexMatcher('/path[0-9]*'))
    def requestWithDifferentPath = request.copy()
    requestWithDifferentPath.path = '/pathA'

    when:
    def actualResponse = test(requestWithDifferentPath)

    then:
    !actualResponse
  }

}
