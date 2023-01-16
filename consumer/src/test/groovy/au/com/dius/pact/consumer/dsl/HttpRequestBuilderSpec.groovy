package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.HttpRequest
import kotlin.Pair
import spock.lang.Specification

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


}
