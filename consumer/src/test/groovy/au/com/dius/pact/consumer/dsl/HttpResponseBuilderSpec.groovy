package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.HttpResponse
import kotlin.Pair
import spock.lang.Specification

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


}
