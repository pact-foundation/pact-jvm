package au.com.dius.pact.consumer

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.Response
import spock.lang.Specification
import spock.lang.Unroll

class MockServerLogSpec extends Specification {

  // -------------------------------------------------------------------------
  // requestToString
  // -------------------------------------------------------------------------

  def 'requestToString - simple GET with no headers or body'() {
    given:
    def request = new Request('GET', '/path')

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '>>> GET /path'
  }

  def 'requestToString - includes query parameters'() {
    given:
    def request = new Request('GET', '/search', ['q': ['pact'], 'page': ['2']])

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '>>> GET /search?q=pact&page=2'
  }

  def 'requestToString - multi-value query parameter'() {
    given:
    def request = new Request('GET', '/items', ['id': ['1', '2', '3']])

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '>>> GET /items?id=1&id=2&id=3'
  }

  def 'requestToString - includes headers'() {
    given:
    def request = new Request('DELETE', '/resource', [:],
      ['Accept': ['application/json'], 'X-Request-Id': ['abc-123']])

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '''\
>>> DELETE /resource
  Accept: application/json
  X-Request-Id: abc-123'''
  }

  def 'requestToString - multi-value header is joined with commas'() {
    given:
    def request = new Request('GET', '/', [:],
      ['Accept': ['application/json', 'text/plain']])

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '''\
>>> GET /
  Accept: application/json, text/plain'''
  }

  def 'requestToString - missing body produces no body block'() {
    given:
    def request = new Request('GET', '/', [:], [:], OptionalBody.missing())

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '>>> GET /'
  }

  def 'requestToString - empty body is labelled'() {
    given:
    def request = new Request('POST', '/api', [:], [:], OptionalBody.empty())

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '''\
>>> POST /api

  (empty body)'''
  }

  def 'requestToString - null body is labelled'() {
    given:
    def request = new Request('POST', '/api', [:], [:], OptionalBody.nullBody())

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '''\
>>> POST /api

  (null body)'''
  }

  def 'requestToString - JSON body is pretty-printed'() {
    given:
    def body = OptionalBody.body('{"name":"Alice","id":1}'.bytes, ContentType.JSON)
    def request = new Request('POST', '/users', [:],
      ['Content-Type': ['application/json']], body)

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '''\
>>> POST /users
  Content-Type: application/json

  {
    "id": 1,
    "name": "Alice"
  }'''
  }

  def 'requestToString - text body is indented'() {
    given:
    def body = OptionalBody.body('hello world'.bytes, new ContentType('text/plain'))
    def request = new Request('POST', '/notes', [:], [:], body)

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '''\
>>> POST /notes

  hello world'''
  }

  def 'requestToString - multiline text body each line is indented'() {
    given:
    def body = OptionalBody.body('line one\nline two'.bytes, new ContentType('text/plain'))
    def request = new Request('POST', '/notes', [:], [:], body)

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '''\
>>> POST /notes

  line one
  line two'''
  }

  def 'requestToString - XML body is indented'() {
    given:
    def body = OptionalBody.body('<root/>'.bytes, ContentType.XML)
    def request = new Request('POST', '/data', [:], [:], body)

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '''\
>>> POST /data

  <root/>'''
  }

  def 'requestToString - binary body shows byte count'() {
    given:
    def body = OptionalBody.body(new byte[42], new ContentType('application/octet-stream'))
    def request = new Request('PUT', '/upload', [:], [:], body)

    expect:
    MockServerLog.INSTANCE.requestToString(request) == '''\
>>> PUT /upload

  (42 bytes of binary data)'''
  }

  // -------------------------------------------------------------------------
  // responseToString
  // -------------------------------------------------------------------------

  def 'responseToString - simple response with no headers or body'() {
    given:
    def response = new Response(204)

    expect:
    MockServerLog.INSTANCE.responseToString(response) == '<<< 204 No Content'
  }

  def 'responseToString - unknown status code has no description'() {
    given:
    def response = new Response(999)

    expect:
    MockServerLog.INSTANCE.responseToString(response) == '<<< 999'
  }

  def 'responseToString - includes headers'() {
    given:
    def response = new Response(200, ['Content-Type': ['application/json'], 'X-Trace-Id': ['xyz']])

    expect:
    MockServerLog.INSTANCE.responseToString(response) == '''\
<<< 200 OK
  Content-Type: application/json
  X-Trace-Id: xyz'''
  }

  def 'responseToString - JSON body is pretty-printed'() {
    given:
    def body = OptionalBody.body('{"id":42,"name":"Bob"}'.bytes, ContentType.JSON)
    def response = new Response(200, ['Content-Type': ['application/json']], body)

    expect:
    MockServerLog.INSTANCE.responseToString(response) == '''\
<<< 200 OK
  Content-Type: application/json

  {
    "id": 42,
    "name": "Bob"
  }'''
  }

  def 'responseToString - invalid JSON falls back to raw string'() {
    given:
    def body = OptionalBody.body('not-valid-json'.bytes, ContentType.JSON)
    def response = new Response(200, [:], body)

    expect:
    MockServerLog.INSTANCE.responseToString(response) == '''\
<<< 200 OK

  not-valid-json'''
  }

  def 'responseToString - text body is indented'() {
    given:
    def body = OptionalBody.body('plain text response'.bytes, new ContentType('text/plain'))
    def response = new Response(200, [:], body)

    expect:
    MockServerLog.INSTANCE.responseToString(response) == '''\
<<< 200 OK

  plain text response'''
  }

  def 'responseToString - binary body shows byte count'() {
    given:
    def body = OptionalBody.body(new byte[256], new ContentType('image/png'))
    def response = new Response(200, [:], body)

    expect:
    MockServerLog.INSTANCE.responseToString(response) == '''\
<<< 200 OK

  (256 bytes of binary data)'''
  }

  def 'responseToString - empty body is labelled'() {
    given:
    def response = new Response(200, [:], OptionalBody.empty())

    expect:
    MockServerLog.INSTANCE.responseToString(response) == '''\
<<< 200 OK

  (empty body)'''
  }

  def 'responseToString - null body is labelled'() {
    given:
    def response = new Response(200, [:], OptionalBody.nullBody())

    expect:
    MockServerLog.INSTANCE.responseToString(response) == '''\
<<< 200 OK

  (null body)'''
  }

  @Unroll
  def 'httpStatusDescription covers common status code #status'() {
    given:
    def response = new Response(status)

    expect:
    MockServerLog.INSTANCE.responseToString(response).startsWith("<<< $status $description")

    where:
    status | description
    200    | 'OK'
    201    | 'Created'
    204    | 'No Content'
    400    | 'Bad Request'
    401    | 'Unauthorized'
    403    | 'Forbidden'
    404    | 'Not Found'
    409    | 'Conflict'
    422    | 'Unprocessable Entity'
    500    | 'Internal Server Error'
    503    | 'Service Unavailable'
  }
}
