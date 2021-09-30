package au.com.dius.pact.core.matchers

import spock.lang.Specification
import au.com.dius.pact.core.model.ContentType

class ContentTypeMatcherSpec extends Specification {
  def 'matching binary data where content type matches'() {
    given:
    def path = []
    def contentType = ContentType.fromString('application/pdf')
    def actual = ContentTypeMatcherSpec.getResourceAsStream('/sample.pdf').bytes
    def mismatchFactory = [create: { p1, p2, message, p3 ->
      new BodyMismatch(p1, p2, message, 'path')
    }] as MismatchFactory

    when:
    def result = MatcherExecutorKt.matchHeaderWithParameters(path, contentType, actual, mismatchFactory)

    then:
    result.empty
  }

  def 'matching binary data where content type does not match'() {
    given:
    def path = []
    def contentType = ContentType.fromString('application/pdf')
    def actual = '"I\'m a PDF!"'.bytes
    def mismatchFactory = [create: { p1, p2, message, p3 ->
      new BodyMismatch(p1, p2, message, 'path')
    }] as MismatchFactory

    when:
    def result = MatcherExecutorKt.matchHeaderWithParameters(path, contentType, actual, mismatchFactory)

    then:
    !result.empty
    result*.mismatch == [
      'Expected binary contents to have content type \'application/pdf\' but detected contents was \'application/json\''
    ]
  }

  def 'matching binary data with a text format like JSON'() {
    given:
    def path = []
    def contentType = ContentType.fromString('application/json')
    def actual = '["I\'m a PDF!"]'.bytes
    def mismatchFactory = [create: { p1, p2, message, p3 ->
      new BodyMismatch(p1, p2, message, 'path')
    }] as MismatchFactory

    when:
    def result = MatcherExecutorKt.matchHeaderWithParameters(path, contentType, actual, mismatchFactory)

    then:
    result.empty
  }
}
