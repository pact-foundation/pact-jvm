package au.com.dius.pact.provider.groovysupport

import au.com.dius.pact.model.Request
import au.com.dius.pact.provider.ProviderClient
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpRequest
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import spock.lang.Specification
import spock.lang.Unroll

class ProviderClientSpec extends Specification {

  private ProviderClient client
  private provider
  private HttpRequest httpRequest

  def setup() {
    provider = [
      protocol: 'http',
      host: 'localhost',
      port: 8080,
      path: '/'
    ]
    client = new ProviderClient(provider: provider)
    httpRequest = Mock HttpRequest
  }

  def 'setting up headers does nothing if there are no headers'() {
    given:
    client.request = new Request('PUT', '/', null, null, null, [:])

    when:
    client.setupHeaders(httpRequest)

    then:
    0 * httpRequest._
  }

  def 'setting up headers copies all headers without modification'() {
    given:
    def headers = [
      'Content-Type': ContentType.APPLICATION_ATOM_XML.toString(),
      A: 'a',
      B: 'b',
      C: 'c'
    ]
    client.request = new Request('PUT', '/', null, headers, null, [:])

    when:
    client.setupHeaders(httpRequest)

    then:
    1 * httpRequest.containsHeader('Content-Type') >> true
    headers.each {
      1 * httpRequest.addHeader(it.key, it.value)
    }

    0 * httpRequest._
  }

  def 'setting up headers adds an JSON content type if none was provided'() {
    given:
    def headers = [
      A: 'a',
      B: 'b',
      C: 'c'
    ]
    client.request = new Request('PUT', '/', null, headers, null, [:])

    when:
    client.setupHeaders(httpRequest)

    then:
    1 * httpRequest.containsHeader('Content-Type') >> false
    headers.each {
      1 * httpRequest.addHeader(it.key, it.value)
    }
    1 * httpRequest.addHeader('Content-Type', 'application/json')

    0 * httpRequest._
  }

  def 'setting up body does nothing if the request is not an instance of HttpEntityEnclosingRequest'() {
    when:
    client.setupBody(httpRequest)

    then:
    0 * httpRequest._
  }

  def 'setting up body does nothing if it is not a post and there is no body'() {
    given:
    httpRequest = Mock HttpEntityEnclosingRequest
    client.request = new Request('PUT', '/', null, null, null, [:])

    when:
    client.setupBody(httpRequest)

    then:
    0 * httpRequest._
  }

  def 'setting up body sets a string entity if it is not a url encoded form post and there is a body'() {
    given:
    httpRequest = Mock HttpEntityEnclosingRequest
    client.request = new Request('PUT', '/', null, null, '{}', [:])

    when:
    client.setupBody(httpRequest)

    then:
    1 * httpRequest.setEntity { it instanceof StringEntity && it.content.text == '{}' }
    0 * httpRequest._
  }

  def 'setting up body sets a string entity if it is a url encoded form post and there is no query string'() {
    given:
    httpRequest = Mock HttpEntityEnclosingRequest
    client.request = new Request('POST', '/', null, ['Content-Type': ContentType.APPLICATION_FORM_URLENCODED.mimeType],
      'A=B', [:])

    when:
    client.setupBody(httpRequest)

    then:
    1 * httpRequest.setEntity { it instanceof StringEntity && it.content.text == 'A=B' }
    0 * httpRequest._
  }

  def 'setting up body sets a UrlEncodedFormEntity entity if it is urlencoded form post and there is a query string'() {
    given:
    httpRequest = Mock HttpEntityEnclosingRequest
    client.request = new Request('POST', '/', ['A': ['B', 'C']], ['Content-Type': 'application/x-www-form-urlencoded'],
      '{}', [:])

    when:
    client.setupBody(httpRequest)

    then:
    1 * httpRequest.setEntity { it instanceof UrlEncodedFormEntity && it.content.text == 'A=B&A=C' }
    0 * httpRequest._
  }

  @Unroll
  @SuppressWarnings('UnnecessaryBooleanExpression')
  def 'request is a url encoded form post'() {
    expect:
    def request = new Request(method, '/', ['A': ['B', 'C']], ['Content-Type': contentType], '{}', [:])
    ProviderClient.urlEncodedFormPost(request) == urlEncodedFormPost

    where:
    method      | contentType                         || urlEncodedFormPost
    'POST'      | 'application/x-www-form-urlencoded' || true
    'post'      | 'application/x-www-form-urlencoded' || true
    'PUT'       | 'application/x-www-form-urlencoded' || false
    'GET'       | 'application/x-www-form-urlencoded' || false
    'OPTION'    | 'application/x-www-form-urlencoded' || false
    'HEAD'      | 'application/x-www-form-urlencoded' || false
    'PATCH'     | 'application/x-www-form-urlencoded' || false
    'DELETE'    | 'application/x-www-form-urlencoded' || false
    'TRACE'     | 'application/x-www-form-urlencoded' || false
    'POST'      | 'application/javascript'            || false
  }

  def 'execute request filter does nothing if there is no request filter'() {
    given:
    provider.requestFilter = null

    when:
    client.executeRequestFilter(httpRequest)

    then:
    0 * _
  }

  def 'execute request filter executes any groovy closure'() {
    given:
    Boolean closureCalled = false
    provider.requestFilter = { request ->
      closureCalled = true
      httpRequest.addHeader('A', 'B')
    }

    when:
    client.executeRequestFilter(httpRequest)

    then:
    closureCalled
    1 * httpRequest.addHeader('A', 'B')
    0 * _
  }

  def 'execute request filter executes any scala closure'() {
    given:
    provider.requestFilter = GroovyScalaUtils$.MODULE$.testRequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * httpRequest.addHeader('Scala', 'Was Called')
    0 * _
  }

  def 'execute request filter defaults to executing a groovy script'() {
    given:
    provider.requestFilter = 'request.addHeader("Groovy", "Was Called")'

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * httpRequest.addHeader('Groovy', 'Was Called')
    0 * _
  }

  def 'execute request filter executes any Java Consumer'() {
    given:
    provider.requestFilter = GroovyJavaUtils.consumerRequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * httpRequest.addHeader('Java Consumer', 'was called')
    0 * _
  }

  def 'execute request filter executes a Java Function'() {
    given:
    provider.requestFilter = GroovyJavaUtils.functionRequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * httpRequest.addHeader('Java Function', 'was called')
    0 * _
  }

  def 'execute request filter executes any Java Function'() {
    given:
    provider.requestFilter = GroovyJavaUtils.function2RequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * httpRequest.addHeader('Java Function', 'was called')
    0 * _
  }

  def 'execute request filter throws an exception with parameters in a different order'() {
    given:
    provider.requestFilter = GroovyJavaUtils.function2RequestFilterWithParametersSwapped()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    thrown(RuntimeException)
    0 * _
  }

  def 'execute request filter throws an exception invalid Java Function parameters'() {
    given:
    provider.requestFilter = GroovyJavaUtils.invalidFunction2RequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    thrown(RuntimeException)
    0 * _
  }

  def 'execute request filter throws an exception for invalid java functions'() {
    given:
    provider.requestFilter = GroovyJavaUtils.supplierRequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    thrown(IllegalArgumentException)
    0 * _
  }

}
