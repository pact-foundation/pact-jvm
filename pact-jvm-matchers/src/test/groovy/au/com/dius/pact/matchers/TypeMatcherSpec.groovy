package au.com.dius.pact.matchers

@SuppressWarnings(['UnusedImport', 'DuplicateImport'])
import au.com.dius.pact.model.DiffConfig
import au.com.dius.pact.model.DiffConfig$
import au.com.dius.pact.model.Request
import spock.lang.Specification

class TypeMatcherSpec extends Specification {

  private final DiffConfig diffConfig = DiffConfig$.MODULE$.apply(true, false)

  def 'match integers should accept integer values'() {
    given:
    def expected = new Request('get', '/', null, null, '{"value": 123}', ['$.body.value': [match: 'integer']])
    def actual = new Request('get', '/', null, null, '{"value": 456}', null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, diffConfig)

    then:
    result.empty
  }

  def 'match integers should null values'() {
    given:
    def expected = new Request('get', '/', null, null, '{"value": 123}', ['$.body.value': [match: 'integer']])
    def actual = new Request('get', '/', null, null, '{"value": null}', null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, diffConfig)

    then:
    !result.empty
  }

  def 'match integers should fail for non-integer values'() {
    given:
    def expected = new Request('get', '/', null, null, '{"value": 123}', ['$.body.value': [match: 'integer']])
    def actual = new Request('get', '/', null, null, '{"value": 123.10}', null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, diffConfig)

    then:
    !result.empty
  }

  def 'match decimal should accept decimal values'() {
    given:
    def expected = new Request('get', '/', null, null, '{"value": 123.10}', ['$.body.value': [match: 'decimal']])
    def actual = new Request('get', '/', null, null, '{"value": 456.20}', null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, diffConfig)

    then:
    result.empty
  }

  def 'match decimal should handle null values'() {
    given:
    def expected = new Request('get', '/', null, null, '{"value": 123.10}', ['$.body.value': [match: 'decimal']])
    def actual = new Request('get', '/', null, null, '{"value": null}', null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, diffConfig)

    then:
    !result.empty
  }

  def 'match decimal should fail for non-decimal values'() {
    given:
    def expected = new Request('get', '/', null, null, '{"value": 123.10}', ['$.body.value': [match: 'decimal']])
    def actual = new Request('get', '/', null, null, '{"value": 123}', null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, diffConfig)

    then:
    !result.empty
  }

}
