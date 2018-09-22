package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import spock.lang.Specification

class MultipartMessageBodyMatcherSpec extends Specification {

  private MultipartMessageBodyMatcher matcher
  private expected, actual

  def setup() {
    matcher = new MultipartMessageBodyMatcher()
    expected = { body -> new Request('', '', null, ['Content-Type': 'multipart/form-data; boundary=XXX'], body) }
    actual = { body -> new Request('', '', null, ['Content-Type': 'multipart/form-data; boundary=XXX'], body) }
  }

  def 'return no mismatches - when comparing empty bodies'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.empty()
  }

  def 'return no mismatches - when comparing a missing body to anything'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('"Blah"')
    expectedBody = OptionalBody.missing()
  }

  def 'returns a mismatch - when comparing anything to an empty body'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true)*.mismatch == [
      'Expected a multipart body but was missing'
    ]

    where:

    actualBody = OptionalBody.body('')
    expectedBody = OptionalBody.body('"Blah"')
  }

  def 'returns a mismatch - when the actual body is missing a header'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true)*.mismatch == [
      'Expected a multipart header \'Test\', but was missing'
    ]

    where:

    actualBody = multipart('form-data', 'file', '476.csv', 'text/plain', '', '1234')
    expectedBody = multipart('form-data', 'file', '476.csv', 'text/plain', 'Test: true\n', '1234')
  }

  def 'returns a mismatch - when the headers do not match'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true)*.mismatch == [
      'Expected a multipart header \'Content-Type\' with value \'text/html\', but was \'text/plain\''
    ]

    where:

    actualBody = multipart('form-data', 'file', '476.csv', 'text/plain', 'Test: true\n', '1234')
    expectedBody = multipart('form-data', 'file', '476.csv', 'text/html', 'Test: true\n', '1234')
  }

  def 'returns a mismatch - when the actual body is empty'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true)*.mismatch == [
      'Expected content with the multipart, but received no bytes of content'
    ]

    where:

    actualBody = multipart('form-data', 'file', '476.csv', 'text/plain', '',
      '')
    expectedBody = multipart('form-data', 'file', '476.csv', 'text/plain', '',
      '1234')
  }

  @SuppressWarnings('ParameterCount')
  OptionalBody multipart(disposition, name, filename, contentType, headers, body) {
    OptionalBody.body(

      """--XXX
        |Content-Disposition: $disposition; name=\"$name\"; filename=\"$filename\"
        |Content-Type: $contentType
        |$headers
        |
        |$body
        |--XXX
       """.stripMargin()
    )
  }

}
