package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher
import au.com.dius.pact.core.model.matchingrules.IgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

@SuppressWarnings(['BracesForMethod', 'PrivateFieldCouldBeFinal'])
class JsonBodyMatcherSpec extends Specification {

  private matchers
  private JsonBodyMatcher matcher = new JsonBodyMatcher()

  def setup() {
    matchers = new MatchingRulesImpl()
  }

  def 'matching json bodies - return no mismatches - when comparing empty bodies'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.empty()
  }

  def 'matching json bodies - return no mismatches - when comparing a missing body to anything'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('"Blah"'.bytes)
    expectedBody = OptionalBody.missing()
  }

  def 'matching json bodies - return no mismatches - with equal bodies'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('"Blah"'.bytes)
    expectedBody = OptionalBody.body('"Blah"'.bytes)
  }

  def 'matching json bodies - return no mismatches - with equal Maps'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('{"something": 100}'.bytes)
    expectedBody = OptionalBody.body('{"something":100}'.bytes)
  }

  def 'matching json bodies - return no mismatches - with equal Lists'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('[100,200,300]'.bytes)
    expectedBody = OptionalBody.body('[100, 200, 300]'.bytes)
  }

  def 'matching json bodies - return no mismatches - with each like matcher on unequal lists'() {
    given:
    matchers.addCategory('body').addRule('$.list', new MinTypeMatcher(1))

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('{"list": [100, 200, 300, 400]}'.bytes)
    expectedBody = OptionalBody.body('{"list": [100]}'.bytes)
  }

  def 'matching json bodies - return no mismatches - with each like matcher on empty list'() {
    given:
    matchers.addCategory('body').addRule('$.list', new MinTypeMatcher(0))

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('{"list": []}'.bytes)
    expectedBody = OptionalBody.body('{"list": [100]}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing anything to an empty body'() {
    expect:
    !matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body(''.bytes)
    expectedBody = OptionalBody.body('"Blah"'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing anything to a null body'() {
    expect:
    !matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('""'.bytes)
    expectedBody = OptionalBody.nullBody()
  }

  def 'matching json bodies - returns no mismatch - when comparing an empty map to a non-empty one'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('{"something": 100}'.bytes)
    expectedBody = OptionalBody.body('{}'.bytes)
  }

  def '''matching json bodies - returns a mismatch - when comparing an empty map to a non-empty one and we do not
         allow unexpected keys'''() {
    expect:
    matcher.matchBody(expectedBody, actualBody, false, matchers).find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Expected an empty Map but received {"something":100}')
    }

    where:

    actualBody = OptionalBody.body('{"something": 100}'.bytes)
    expectedBody = OptionalBody.body('{}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing an empty list to a non-empty one'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Expected an empty List but received [100]')
    }

    where:

    actualBody = OptionalBody.body('[100]'.bytes)
    expectedBody = OptionalBody.body('[]'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing a map to one with less entries'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Expected a Map with at least 2 elements but received 1 elements')
    }

    where:

    actualBody = OptionalBody.body('{"something": 100}'.bytes)
    expectedBody = OptionalBody.body('{"something": 100, "somethingElse": 100}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing a list to one with with different size'() {
    given:
    def actualBody = OptionalBody.body('[1,2,3]'.bytes)
    def expectedBody = OptionalBody.body('[1,2,3,4]'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, true, matchers).findAll {
      it instanceof BodyMismatch
    }*.mismatch

    then:
    mismatches.size() == 2
    mismatches.contains('Expected a List with 4 elements but received 3 elements')
    mismatches.contains('Expected 4 but was missing')
  }

  def 'matching json bodies - returns a mismatch - when the actual body is missing a key'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Expected somethingElse=100 but was missing')
    }

    where:

    actualBody = OptionalBody.body('{"something": 100}'.bytes)
    expectedBody = OptionalBody.body('{"something": 100, "somethingElse": 100}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when the actual body has invalid value'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Expected 100 but received 101')
    }

    where:

    actualBody = OptionalBody.body('{"something": 101}'.bytes)
    expectedBody = OptionalBody.body('{"something": 100}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing a map to a list'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Type mismatch: Expected Map {"something":100,"somethingElse":100} ' +
          'but received List [100,100]')
    }

    where:

    actualBody = OptionalBody.body('[100, 100]'.bytes)
    expectedBody = OptionalBody.body('{"something": 100, "somethingElse": 100}'.bytes)
  }

  def 'matching json bodies - returns a mismatch - when comparing list to anything'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).find {
      it instanceof BodyMismatch &&
        it.mismatch.contains('Type mismatch: Expected List [100,100] but received Primitive 100')
    }

    where:

    actualBody = OptionalBody.body('100'.bytes)
    expectedBody = OptionalBody.body('[100, 100]'.bytes)
  }

  def 'matching json bodies - with a matcher defined - delegate to the matcher'() {
    given:
    matchers.addCategory('body').addRule('$.something', new RegexMatcher('\\d+'))

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('{"something": 100}'.bytes)
    expectedBody = OptionalBody.body('{"something": 101}'.bytes)
  }

  @RestoreSystemProperties
  def 'matching json bodies - with a matcher defined - and when the actual body is missing a key, not be a mismatch'() {
    given:
    matchers.addCategory('body').addRule('$.*', TypeMatcher.INSTANCE)
    System.setProperty(Matchers.PACT_MATCHING_WILDCARD, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('{"something": 100, "other": 100}'.bytes)
    expectedBody = OptionalBody.body('{"somethingElse": 100}'.bytes)
  }

  @RestoreSystemProperties
  def 'matching json bodies - with a matcher defined - defect 562: matching a list at the root with extra fields'() {
    given:
    matchers.addCategory('body').addRule('$', new MinTypeMatcher(1))
    matchers.addCategory('body').addRule('$[*].*', TypeMatcher.INSTANCE)
    System.setProperty(Matchers.PACT_MATCHING_WILDCARD, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('''[
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
    ]'''.bytes)
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
    matchers.addCategory('body').addRule('$.*', new MinTypeMatcher(0))
    System.setProperty(Matchers.PACT_MATCHING_WILDCARD, 'false')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).find {
      it instanceof BodyMismatch && it.mismatch.contains('Expected height=100 but was missing')
    }

    where:

    actualBody = OptionalBody.body('{"id": 100, "width": 100}'.bytes)
    expectedBody = OptionalBody.body('{"id": 100, "height": 100}'.bytes)
  }

  @RestoreSystemProperties
  def 'returns no mismatch - when comparing maps with different keys and wildcard matching is enabled'() {
    given:
    matchers.addCategory('body').addRule('$.*', new MinTypeMatcher(0))
    System.setProperty(Matchers.PACT_MATCHING_WILDCARD, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:

    actualBody = OptionalBody.body('{"id": 100, "width": 100}'.bytes)
    expectedBody = OptionalBody.body('{"id": 100, "height": 100}'.bytes)
  }

  @RestoreSystemProperties
  def '''matching json bodies - return no mismatches - when comparing a list of numbers and ignore-order
         matching is enabled'''() {
    given:
    matchers.addCategory('body')
      .addRule('$.array1', IgnoreOrderMatcher.INSTANCE)
      .addRule('$.array1[*]', TypeMatcher.INSTANCE)
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:
    actualBody = OptionalBody.body('{"array1": [2, 3, 1, 4]}'.bytes)
    expectedBody = OptionalBody.body('{"array1": [1, 2, 3, 4]}'.bytes)
  }

  @RestoreSystemProperties
  def '''matching json bodies - return no mismatches - when comparing a list of objects and ignore-order
         matching is enabled'''() {
    given:
    matchers.addCategory('body')
            .addRule('$.array1', IgnoreOrderMatcher.INSTANCE)
            .addRule('$.array1[*].foo', new RegexMatcher('a|b'))
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:
    actualBody = OptionalBody.body('{"array1": [{"foo": "a"},{"foo": "b"}]}'.bytes)
    expectedBody = OptionalBody.body('{"array1": [{"foo": "b"},{"foo": "a"}]}'.bytes)
  }

  @RestoreSystemProperties
  def '''matching json bodies - return mismatches - when comparing a list of objects, ignore-order
         matching is enabled and actual is smaller than expected'''() {
    given:
    matchers.addCategory('body')
            .addRule('$.array1', IgnoreOrderMatcher.INSTANCE)
            .addRule('$.array1[*].foo', new RegexMatcher('a|b|c'))
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).find {
      it instanceof BodyMismatch && it.mismatch.contains('Expected [{"foo":"a"},{"foo":"b"}] to equal ' +
              '[{"foo":"b"},{"foo":"a"},{"foo":"c"}] ignoring order of elements')
    }

    where:
    actualBody = OptionalBody.body('{"array1": [{"foo": "a"},{"foo": "b"}]}'.bytes)
    expectedBody = OptionalBody.body('{"array1": [{"foo": "b"},{"foo": "a"},{"foo": "c"}]}'.bytes)
  }

  @RestoreSystemProperties
  def '''matching json bodies - return no mismatches - when comparing two lists of objects, one with
         ignore-order matching enabled and one not enabled'''() {
    given:
    matchers.addCategory('body')
            .addRule('$[0].array1', IgnoreOrderMatcher.INSTANCE)
            .addRule('$[0].array1[*].foo', new RegexMatcher('a|b'))
            .addRule('$[1].array2[*]', TypeMatcher.INSTANCE)
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:
    actualBody = OptionalBody.body('''[
     {"array1": [{"foo": "a"},{"foo": "b"}]},
     {"array2": [2, 3, 3, 4]}
     ]'''.bytes)
    expectedBody = OptionalBody.body('''[
     {"array1": [{"foo": "b"},{"foo": "a"}]},
     {"array2": [1, 2, 3, 4]}
     ]'''.bytes)
  }

  @RestoreSystemProperties
  def '''matching json bodies - return mismatches - when comparing two lists of objects, one with
         ignore-order matching enabled and one not enabled'''() {
    given:
    matchers.addCategory('body')
            .addRule('$[0].array1', IgnoreOrderMatcher.INSTANCE)
            .addRule('$[0].array1[*].foo', new RegexMatcher('a|b'))
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).find {
      it instanceof BodyMismatch && it.mismatch.contains('Expected 1 but received 2')
    }

    where:
    actualBody = OptionalBody.body('''[
     {"array1": [{"foo": "a"},{"foo": "b"}]},
     {"array2": [2, 3, 1, 4]}
     ]'''.bytes)
    expectedBody = OptionalBody.body('''[
     {"array1": [{"foo": "b"},{"foo": "a"}]},
     {"array2": [1, 2, 3, 4]}
     ]'''.bytes)
  }

  @RestoreSystemProperties
  def '''matching json bodies - returns a mismatch - when comparing a list to one with different
         size and ignore-order matching enabled'''() {
    given:
    matchers.addCategory('body')
            .addRule('$.array1', IgnoreOrderMatcher.INSTANCE)
            .addRule('$.array1[*]', TypeMatcher.INSTANCE)
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')
    def actualBody = OptionalBody.body('{"array1": [2, 1, 3]}'.bytes)
    def expectedBody = OptionalBody.body('{"array1": [1, 2, 3, 4]}'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, true, matchers).findAll {
      it instanceof BodyMismatch
    }*.mismatch

    then:
    mismatches.size() == 1
    mismatches.contains('Expected [2,1,3] to equal [1,2,3,4] ignoring order of elements')
  }

  @RestoreSystemProperties
  def '''matching json bodies - return no mismatches - with each like matcher on unequal lists
         and ignore-order matching enabled'''() {
    given:
    matchers.addCategory('body')
            .addRule('$.array1', new MinTypeMatcher(1))
            .addRule('$.array1', IgnoreOrderMatcher.INSTANCE)
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:
    actualBody = OptionalBody.body('{"array1": [100, 200, 300, 400]}'.bytes)
    expectedBody = OptionalBody.body('{"array1": [200]}'.bytes)
  }

  @RestoreSystemProperties
  def 'matching json bodies - return no mismatches - with multiple of the same elements'() {
    given:
    matchers.addCategory('body')
            .addRule('$.array1', TypeMatcher.INSTANCE)
            .addRule('$.array1', IgnoreOrderMatcher.INSTANCE)
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:
    actualBody = OptionalBody.body('{"array1": [100, 100, 100, 400]}'.bytes)
    expectedBody = OptionalBody.body('{"array1": [100, 100]}'.bytes)
  }

  @RestoreSystemProperties
  def 'matching json bodies - return mismatches - with multiple of the same elements'() {
    given:
    matchers.addCategory('body')
            .addRule('$.array1', TypeMatcher.INSTANCE)
            .addRule('$.array1', IgnoreOrderMatcher.INSTANCE)
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')
    def actualBody = OptionalBody.body('{"array1": [100, 100, 100, 400]}'.bytes)
    def expectedBody = OptionalBody.body('{"array1": [100, 100, 500]}'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, true, matchers).findAll {
      it instanceof BodyMismatch
    }*.mismatch

    then:
    mismatches.size() == 1
    mismatches.contains('Expected [100,100,100,400] to equal [100,100,500] ignoring order of elements')
  }

  @RestoreSystemProperties
  def 'matching json bodies - return no mismatches - with unordered matching of elements with unique ids'() {
    given:
    matchers.addCategory('body')
            .addRule('$.array', IgnoreOrderMatcher.INSTANCE)
            .addRule('$.array[*].id', new EqualsMatcher())
            .addRule('$.array[0].status', new EqualsMatcher())
            .addRule('$.array[1].status', new RegexMatcher('up|down'))
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty == result

    where:
    actualBody << [OptionalBody.body('{"array": [{"id":"b", "status":"up"},{"id":"a", "status":"down"}]}'.bytes),
                   OptionalBody.body('{"array": [{"id":"a", "status":"up"},{"id":"b", "status":"down"}]}'.bytes),
                   OptionalBody.body('{"array": [{"id":"a", "status":"up"},{"id":"b", "status":"up"}]}'.bytes),
                   OptionalBody.body('{"array": [{"id":"b", "status":"down"},{"id":"a", "status":"up"}]}'.bytes)]

    expectedBody << [OptionalBody.body('{"array": [{"id":"a", "status":"up"},{"id":"b", "status":"down"}]}'.bytes),
                     OptionalBody.body('{"array": [{"id":"a", "status":"up"},{"id":"b", "status":"down"}]}'.bytes),
                     OptionalBody.body('{"array": [{"id":"a", "status":"up"},{"id":"b", "status":"down"}]}'.bytes),
                     OptionalBody.body('{"array": [{"id":"a", "status":"up"},{"id":"b", "status":"down"}]}'.bytes)]

    result << [false, true, true, true]
  }

  @RestoreSystemProperties
  def 'matching json bodies - return no mismatches - with unordered matching of mixed elements'() {
    given:
    matchers.addCategory('body')
            .addRule('$.array1', IgnoreOrderMatcher.INSTANCE)
            .addRule('$.array1[*].id', new EqualsMatcher())
            .addRule('$.array1[0].status', new EqualsMatcher())
            .addRule('$.array1[1].status', new RegexMatcher('up|down'))
            .addRule('$.array1[3]', TypeMatcher.INSTANCE)
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty

    where:
    actualBody = OptionalBody.body('{"array1": [{"id":"b", "status":"up"}, {"id":"a", "status":"up"}, 5]}'.bytes)
    expectedBody = OptionalBody.body('{"array1": [{"id":"a", "status":"up"}, 4, {"id":"b", "status":"down"}]}'.bytes)
  }

  @Unroll
  @RestoreSystemProperties
  def 'matching json bodies - return no mismatches - with unordered matching of elements without unique ids'() {
    given:
    matchers.addCategory('body')
            .addRule('$.array', IgnoreOrderMatcher.INSTANCE)
            .addRule('$.array[0].foo', new RegexMatcher('a|b'))
            .addRule('$.array[0].status', new EqualsMatcher())
            .addRule('$.array[1].foo', new RegexMatcher('b|c'))
            .addRule('$.array[1].status', new RegexMatcher('up|down'))
    System.setProperty(Matchers.PACT_MATCHING_IGNORE_ORDER, 'true')

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty == result

    where:
    actualBody << [OptionalBody.body('{"array": [{"foo":"b", "status":"up"},{"foo":"b", "status":"down"}]}'.bytes),
                   OptionalBody.body('{"array": [{"foo":"a", "status":"up"},{"foo":"b", "status":"down"}]}'.bytes),
                   OptionalBody.body('{"array": [{"foo":"a", "status":"up"},{"foo":"b", "status":"up"}]}'.bytes),
                   OptionalBody.body('{"array": [{"foo":"b", "status":"down"},{"foo":"a", "status":"up"}]}'.bytes)]

    expectedBody << [OptionalBody.body('{"array": [{"foo":"a", "status":"up"},{"foo":"b", "status":"down"}]}'.bytes),
                     OptionalBody.body('{"array": [{"foo":"a", "status":"up"},{"foo":"b", "status":"down"}]}'.bytes),
                     OptionalBody.body('{"array": [{"foo":"a", "status":"up"},{"foo":"b", "status":"down"}]}'.bytes),
                     OptionalBody.body('{"array": [{"foo":"a", "status":"up"},{"foo":"b", "status":"down"}]}'.bytes)]

    result << [true, true, true, true]
  }

}
