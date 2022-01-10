package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.Category
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.matchingrules.ValuesMatcher
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import static au.com.dius.pact.core.support.json.KafkaSchemaRegistryWireFormatter.addMagicBytes

@SuppressWarnings(['BracesForMethod', 'PrivateFieldCouldBeFinal'])
class KafkaJsonSchemaContentMatcherSpec extends Specification {

  private MatchingRules rules
  private KafkaJsonSchemaContentMatcher matcher = new KafkaJsonSchemaContentMatcher()
  private Category category

  def setup() {
    rules = new MatchingRulesImpl()
    category = rules.addCategory('body')
  }

  def 'matching json bodies - return no mismatches - when comparing empty bodies'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.empty()
  }

  def 'matching json bodies - return no mismatches - when comparing a missing body to anything'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes('"Blah"'.bytes))
    expectedBody = OptionalBody.missing()
  }

  def 'matching json bodies - return no mismatches - with equal bodies'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes('"Blah"'.bytes))
    expectedBody = OptionalBody.body('"Blah"'.bytes)
  }

  def 'matching json bodies - return no mismatches - with equal Maps'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"something": 100}'.bytes))
    expectedBody = OptionalBody.body('{"something":100}'.bytes)
  }

  def 'matching json bodies - return no mismatches - with equal Lists'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes('[100,200,300]'.bytes))
    expectedBody = OptionalBody.body('[100, 200, 300]'.bytes)
  }

  def 'matching json bodies - return no mismatches - with each like matcher on unequal lists'() {
    given:
    category.addRule('$.list', new MinTypeMatcher(1))

    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"list": [100, 200, 300, 400]}'.bytes))
    expectedBody = OptionalBody.body('{"list": [100]}'.bytes)
  }

  def 'matching json bodies - return no mismatches - with each like matcher on empty list'() {
    given:
    category.addRule('$.list', new MinTypeMatcher(0))

    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"list": []}'.bytes))
    expectedBody = OptionalBody.body('{"list": [100]}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing anything to an empty body'() {
    expect:
    !matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes(''.bytes))
    expectedBody = OptionalBody.body('"Blah"'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing anything to a null body'() {
    expect:
    !matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes('""'.bytes))
    expectedBody = OptionalBody.nullBody()
  }

  def 'matching json bodies - returns no mismatch - when comparing an empty map to a non-empty one'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"something": 100}'.bytes))
    expectedBody = OptionalBody.body('{}'.bytes)
  }

  def '''matching json bodies - returns a mismatch - when comparing an empty map to a non-empty one and we do not
         allow unexpected keys'''() {
    expect:
    matcher.matchBody(expectedBody, actualBody, false, rules).mismatches.find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Expected an empty Map but received {"something":100}')
    }

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"something": 100}'.bytes))
    expectedBody = OptionalBody.body('{}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing an empty list to a non-empty one'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Expected an empty List but received [100]')
    }

    where:
    actualBody = OptionalBody.body(addMagicBytes('[100]'.bytes))
    expectedBody = OptionalBody.body('[]'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing a map to one with less entries'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Expected a Map with at least 2 elements but received 1 elements')
    }

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"something": 100}'.bytes))
    expectedBody = OptionalBody.body('{"something": 100, "somethingElse": 100}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing a list to one with with different size'() {
    given:
    def actualBody = OptionalBody.body(addMagicBytes('[1,2,3]'.bytes))
    def expectedBody = OptionalBody.body('[1,2,3,4]'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.findAll {
      it instanceof BodyMismatch
    }*.mismatch

    then:
    mismatches.size() == 2
    mismatches.contains('Expected a List with 4 elements but received 3 elements')
    mismatches.contains('Expected 4 but was missing')
  }

  def 'matching json bodies - returns a mismatch - when the actual body is missing a key'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Expected a Map with at least 2 elements but received 1 elements')
    }

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"something": 100}'.bytes))
    expectedBody = OptionalBody.body('{"something": 100, "somethingElse": 100}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when the actual body has invalid value'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Expected 100 (Integer) but received 101 (Integer)')
    }

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"something": 101}'.bytes))
    expectedBody = OptionalBody.body('{"something": 100}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing a map to a list'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Type mismatch: Expected Map {"something":100,"somethingElse":100} ' +
          'but received List [100,100]')
    }

    where:

    actualBody = OptionalBody.body(addMagicBytes('[100, 100]'.bytes))
    expectedBody = OptionalBody.body('{"something": 100, "somethingElse": 100}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing list to anything'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Type mismatch: Expected List [100,100] but received Integer 100')
    }

    where:

    actualBody = OptionalBody.body(addMagicBytes('100'.bytes))
    expectedBody = OptionalBody.body('[100, 100]'.bytes)
  }

  def 'matching json bodies - with a matcher defined - delegate to the matcher'() {
    given:
    category.addRule('$.something', new RegexMatcher('\\d+'))

    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"something": 100}'.bytes))
    expectedBody = OptionalBody.body('{"something": 101}'.bytes)
  }

  @SuppressWarnings('LineLength')
  def 'matching json bodies - with a Values matcher defined - and when the actual body is missing a key, not be a mismatch'() {
    given:
    category.addRule('$', ValuesMatcher.INSTANCE)

    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"something": 100, "other": 100}'.bytes))
    expectedBody = OptionalBody.body('{"something": 100}'.bytes)
  }

  @Issue('#562')
  @RestoreSystemProperties
  def 'matching json bodies - with a matcher defined - matching a list at the root with extra fields'() {
    given:
    category.addRule('$', new MinTypeMatcher(1))
    category.addRule('$[*].*', TypeMatcher.INSTANCE)

    when:
    def result = matcher.matchBody(expectedBody, actualBody, true, rules)

    then:
    result.mismatches.size() == 2
    result.mismatches*.description() == [
      "Expected name='Test' but was missing",
      "Expected name='Test' but was missing"
    ]

    where:

    actualBody = OptionalBody.body(addMagicBytes('''[
        {
            "documentId": 0,
            "documentCategoryId": 5,
            "documentCategoryCode": null,
            "contentLength": 0,
            "tags": null
        },
        {
            "documentId": 1,
            "documentCategoryId": 5,
            "documentCategoryCode": null,
            "contentLength": 0,
            "tags": null
        }
    ]'''.bytes))
    expectedBody = OptionalBody.body('''[{
      "name": "Test",
      "documentId": 0,
      "documentCategoryId": 5,
      "contentLength": 0
    }]'''.bytes)
  }

  @RestoreSystemProperties
  def 'returns a mismatch - when comparing maps with different keys and wildcard matching is disabled'() {
    given:
    category.addRule('$.*', new MinTypeMatcher(0))
    System.setProperty(Matchers.PACT_MATCHING_WILDCARD, 'false')

    expect:
    matcher.matchBody(expectedBody, actualBody, false, rules).mismatches.find {
      it instanceof BodyMismatch && it.mismatch.contains(
        'Expected height=100 but was missing')
    }

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"id": 100, "width": 100}'.bytes))
    expectedBody = OptionalBody.body('{"id": 100, "height": 100}'.bytes)
  }

  @RestoreSystemProperties
  def 'returns no mismatch - when comparing maps with different keys and Value matcher is enabled'() {
    given:
    category.addRule('$.*', new MinTypeMatcher(0))
    System.setProperty(Matchers.PACT_MATCHING_WILDCARD, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actualBody = OptionalBody.body(addMagicBytes('{"id": 100, "width": 100}'.bytes))
    expectedBody = OptionalBody.body('{"id": 100, "height": 100}'.bytes)
  }

  @Unroll
  def 'matching json bodies with missing magic bytes - return mismatches - with equal bodies'() {
    expect:
    !matcher.matchBody(expectedBody, actualBody, true, rules).mismatches.empty

    where:

    actual               | expected
    '"Blah"'             | '"Blah"'
    '{"something": 100}' | '{"something": 100}'
    '[100,200,300]'      | '[100,200,300]'

    actualBody = OptionalBody.body(actual.bytes)
    expectedBody = OptionalBody.body(expected.bytes)
  }
}
