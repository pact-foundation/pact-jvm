package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.Charset

class OptionalBodySpec extends Specification {

  @Unroll
  def 'returns the appropriate state for missing'() {
    expect:
    body.missing == value

    where:
    body                    | value
    OptionalBody.missing()  | true
    OptionalBody.empty()    | false
    OptionalBody.nullBody() | false
    OptionalBody.body('a'.bytes)  | false
  }

  @Unroll
  def 'returns the appropriate state for empty'() {
    expect:
    body.empty == value

    where:
    body                         | value
    OptionalBody.missing()       | false
    OptionalBody.empty()         | true
    OptionalBody.body(''.bytes)  | true
    OptionalBody.nullBody()      | false
    OptionalBody.body('a'.bytes) | false
  }

  @Unroll
  def 'returns the appropriate state for nullBody'() {
    expect:
    body.null == value

    where:
    body                         | value
    OptionalBody.missing()       | false
    OptionalBody.empty()         | false
    OptionalBody.nullBody()      | true
    OptionalBody.body(null)      | true
    OptionalBody.body('a'.bytes) | false
  }

  @Unroll
  def 'returns the appropriate state for present'() {
    expect:
    body.present == value

    where:
    body                         | value
    OptionalBody.missing()       | false
    OptionalBody.empty()         | false
    OptionalBody.nullBody()      | false
    OptionalBody.body(''.bytes)  | false
    OptionalBody.body(null)      | false
    OptionalBody.body('a'.bytes) | true
  }

  @Unroll
  def 'returns the appropriate state for not present'() {
    expect:
    body.notPresent == value

    where:
    body                         | value
    OptionalBody.missing()       | true
    OptionalBody.empty()         | true
    OptionalBody.nullBody()      | true
    OptionalBody.body(''.bytes)  | true
    OptionalBody.body(null)      | true
    OptionalBody.body('a'.bytes) | false
  }

  @Unroll
  def 'returns the appropriate value for orElse'() {
    expect:
    body.orElse('default'.bytes) == value.bytes

    where:
    body                         | value
    OptionalBody.missing()       | 'default'
    OptionalBody.empty()         | ''
    OptionalBody.nullBody()      | 'default'
    OptionalBody.body(''.bytes)  | ''
    OptionalBody.body(null)      | 'default'
    OptionalBody.body('a'.bytes) | 'a'
  }

  def 'unwrap throws an exception when the body is missing'() {
    when:
    body.unwrap()

    then:
    thrown(UnwrapMissingBodyException)

    where:
    body << [
      OptionalBody.nullBody(),
      OptionalBody.missing(),
      OptionalBody.body(null)
    ]
  }

  @Unroll
  def 'unwrap does not throw an exception when the body is not missing'() {
    when:
    body.unwrap()

    then:
    notThrown(UnwrapMissingBodyException)

    where:
    body << [
      OptionalBody.empty(),
      OptionalBody.body(''.bytes),
      OptionalBody.body('a'.bytes)
    ]
  }

  @Unroll
  def 'charset test'() {
    expect:
    body.contentType.asCharset() == charset

    where:
    body                                                                               | charset
    OptionalBody.body('{}'.bytes)                                                      | Charset.defaultCharset()
    OptionalBody.body('{}'.bytes, ContentType.UNKNOWN)                                 | Charset.defaultCharset()
    OptionalBody.body('{}'.bytes, ContentType.HTML)                                    | Charset.defaultCharset()
    OptionalBody.body('{}'.bytes, new ContentType('application/json; charset=UTF-16')) | Charset.forName('UTF-16')
  }

  @Unroll
  def 'detect content type test'() {
    expect:
    body.contentType.toString() == contentType

    where:
    body                                               | contentType
    OptionalBody.missing()                             | 'null'
    OptionalBody.body(''.bytes, ContentType.UNKNOWN)   | 'null'
    OptionalBody.body('{}'.bytes, ContentType.UNKNOWN) | 'application/json'
    bodyFromFile('/1070-ApiConsumer-ApiProvider.json') | 'application/json'
    bodyFromFile('/logback-test.xml')                  | 'application/xml'
    bodyFromFile('/RAT.JPG')                           | 'image/jpeg'
  }

  private static OptionalBody bodyFromFile(String file) {
    OptionalBodySpec.getResourceAsStream(file).withCloseable { stream ->
      OptionalBody.body(stream.bytes, ContentType.UNKNOWN)
    }
  }
}
