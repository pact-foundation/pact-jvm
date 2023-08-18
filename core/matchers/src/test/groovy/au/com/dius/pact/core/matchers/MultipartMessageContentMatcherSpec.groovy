package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import spock.lang.Specification

@SuppressWarnings('ThrowRuntimeException')
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

  def 'Ignores missing content type header, which is optional'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty

    where:

    actualBody = multipartFormData('form-data', 'file', '476.csv', null, '', '1234')
    expectedBody = multipartFormData('form-data', 'file', '476.csv', 'text/plain', '', '1234')
  }

  def 'returns a mismatch - when the headers do not match'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch == [
      'Expected a multipart header \'Content-Type\' with value \'text/html\', but was \'text/plain\''
    ]

    where:

    actualBody = multipartFile('file', '476.csv', 'text/plain', '1234')
    expectedBody = multipartFile('file', '476.csv', 'text/html', '1234')
  }

  def 'returns a mismatch - when the actual body is empty'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch == [
      'Expected body \'1234\' to match \'\' using equality but did not match'
    ]

    where:

    actualBody = multipartFile('file', '476.csv', 'text/plain', '')
    expectedBody = multipartFile('file', '476.csv', 'text/plain', '1234')
  }

  def 'returns a mismatch - when the number of parts is different'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch == [
      'Expected a multipart message with 1 part(s), but received one with 2 part(s)'
    ]

    where:

    actualBody = multipart('text/plain', 'This is some text', 'text/plain', 'this is some more text')
    expectedBody = multipart('text/plain', 'This is some text')
  }

  def 'returns a mismatch - when the parts have different content'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch == [
      'Expected \'This is some text\' (String) to be equal to \'This is some other text\' (String)'
    ]

    where:

    actualBody = multipart('application/json', '{"text": "This is some text"}')
    expectedBody = multipart('application/json', '{"text": "This is some other text"}')
  }

  @SuppressWarnings('ParameterCount')
  OptionalBody multipartFile(String name, String filename, String contentType, String body) {
    def builder = MultipartEntityBuilder.create()
    def type = contentType ? org.apache.hc.core5.http.ContentType.parse(contentType) : null
    builder.addBinaryBody(name, body.bytes, type, filename)

    def entity = builder.build()
    OptionalBody.body(entity.content.bytes, new ContentType(entity.contentType))
  }

  OptionalBody multipart(String... partData) {
    if (partData.length % 2 != 0) {
      throw new RuntimeException('multipart requires pairs')
    }

    def builder = MultipartEntityBuilder.create()
    partData.collate(2).eachWithIndex { pair, index ->
      builder.addTextBody("part-$index", pair[1],
        org.apache.hc.core5.http.ContentType.parse(pair[0]))
    }

    def entity = builder.build()
    OptionalBody.body(entity.content.bytes, new ContentType(entity.contentType))
  }

  @SuppressWarnings('ParameterCount')
  OptionalBody multipartFormData(disposition, name, filename, contentType, headers, body) {
    def contentTypeLine = ''
    def headersLine = ''
    if (contentType) {
      contentTypeLine = "Content-Type: $contentType\n"
      if (headers) {
        headersLine = "$contentTypeLine\n$headers\n"
      } else {
        headersLine = contentTypeLine
      }
    } else if (headers) {
      headersLine = headers ?: '\n'
    }
    OptionalBody.body(
      """--XXX
        |Content-Disposition: $disposition; name=\"$name\"; filename=\"$filename\"
        |$headersLine
        |$body
        |--XXX
       """.stripMargin().bytes, new ContentType('multipart/form-data; boundary=XXX')
    )
  }
}
