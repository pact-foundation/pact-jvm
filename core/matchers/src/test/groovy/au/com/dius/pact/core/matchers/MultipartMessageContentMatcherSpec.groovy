package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import spock.lang.Specification

class MultipartMessageContentMatcherSpec extends Specification {

  private MultipartMessageContentMatcher matcher
  private MatchingContext context

  def setup() {
    matcher = new MultipartMessageContentMatcher()
    context = new MatchingContext(new MatchingRuleCategory('body'), true)
  }

  def 'return no mismatches - when comparing empty bodies'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty

    where:

    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.empty()
  }

  def 'return no mismatches - when comparing a missing body to anything'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty

    where:

    actualBody = OptionalBody.body('"Blah"'.bytes)
    expectedBody = OptionalBody.missing()
  }

  def 'returns a mismatch - when comparing anything to an empty body'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch == [
      'Expected a multipart body but was missing'
    ]

    where:

    actualBody = OptionalBody.body(''.bytes)
    expectedBody = OptionalBody.body('"Blah"'.bytes)
  }

  def 'returns a mismatch - when the actual body is missing a header'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch == [
      'Expected a multipart header \'Test\', but was missing'
    ]

    where:

    actualBody = multipart('form-data', 'file', '476.csv', 'text/plain', '', '1234')
    expectedBody = multipart('form-data', 'file', '476.csv', 'text/plain', 'Test: true\n', '1234')
  }

  def 'Ignores missing content type header, which is optional'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty

    where:

    actualBody = multipart('form-data', 'file', '476.csv', null, '', '1234')
    expectedBody = multipart('form-data', 'file', '476.csv', 'text/plain', '', '1234')
  }

  def 'returns a mismatch - when the headers do not match'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch == [
      'Expected a multipart header \'Content-Type\' with value \'text/html\', but was \'text/plain\''
    ]

    where:

    actualBody = multipart('form-data', 'file', '476.csv', 'text/plain', 'Test: true\n', '1234')
    expectedBody = multipart('form-data', 'file', '476.csv', 'text/html', 'Test: true\n', '1234')
  }

  def 'returns a mismatch - when the actual body is empty'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch == [
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
    def contentTypeLine = ''
    def headersLine = ''
    if (contentType) {
      contentTypeLine = "Content-Type: $contentType"
      if (headers) {
        headersLine = "$contentTypeLine\n$headers"
      } else {
        headersLine = contentTypeLine
      }
    } else if (headers) {
      headersLine = headers
    }
    OptionalBody.body(
      """--XXX
        |Content-Disposition: $disposition; name=\"$name\"; filename=\"$filename\"
        |$headersLine
        |
        |$body
        |--XXX
       """.stripMargin().bytes, new ContentType('multipart/form-data; boundary=XXX')
    )
  }
}
