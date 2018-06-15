package au.com.dius.pact.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.model.Request
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import spock.lang.Specification

@SuppressWarnings(['LineLength', 'PrivateFieldCouldBeFinal'])
class XmlBodyMatcherSpec extends Specification {

  private OptionalBody expectedBody, actualBody
  private MatchingRulesImpl matchers
  private expected = { new Request('', '', null, null, expectedBody, matchers) }
  private actual = { new Request('', '', null, null, actualBody) }

  private XmlBodyMatcher matcher

  def setup() {
    matcher = new XmlBodyMatcher()
    matchers = new MatchingRulesImpl()
    expectedBody = OptionalBody.missing()
    actualBody = OptionalBody.missing()
  }

  def 'matching XML bodies - when comparing missing bodies'() {
    expect:
    matcher.matchBody(expected(), actual(), false).empty
  }

  def 'matching XML bodies - when comparing empty bodies'() {
    given:
    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.empty()

    expect:
    matcher.matchBody(expected(), actual(), false).empty
  }

  def 'matching XML bodies - when comparing a missing body to anything'() {
    given:
    actualBody = OptionalBody.body('Blah')

    expect:
    matcher.matchBody(expected(), actual(), false).empty
  }

  def 'matching XML bodies - with equal bodies'() {
    given:
    actualBody = OptionalBody.body('<blah/>')
    expectedBody = OptionalBody.body('<blah/>')

    expect:
    matcher.matchBody(expected(), actual(), false).empty
  }

  def 'matching XML bodies - when bodies differ only in whitespace'() {
    given:
    actualBody = OptionalBody.body(
          '''<foo>
            |  <bar></bar>
            |</foo>
          '''.stripMargin())
    expectedBody = OptionalBody.body('<foo><bar></bar></foo>')

    expect:
    matcher.matchBody(expected(), actual(), false).empty
  }

  def 'matching XML bodies - when allowUnexpectedKeys is true - and comparing an empty list to a non-empty one'() {
    given:
    actualBody = OptionalBody.body('<foo><item/></foo>')
    expectedBody = OptionalBody.body('<foo></foo>')

    expect:
    matcher.matchBody(expected(), actual(), true).empty
  }

  def 'matching XML bodies - when allowUnexpectedKeys is true - and comparing a list to a super-set'() {
    given:
    actualBody = OptionalBody.body('<foo><item1/><item2/></foo>')
    expectedBody = OptionalBody.body('<foo><item1/></foo>')

    expect:
    matcher.matchBody(expected(), actual(), true).empty
  }

  def 'matching XML bodies - when allowUnexpectedKeys is true - and comparing a tags attributes to one with more entries'() {
    given:
    actualBody = OptionalBody.body('<foo something="100" somethingElse="101"/>')
    expectedBody = OptionalBody.body('<foo something="100"/>')

    expect:
    matcher.matchBody(expected(), actual(), true).empty
  }

  def 'matching XML bodies - returns a mismatch - when comparing anything to an empty body'() {
    given:
    expectedBody = OptionalBody.body('<blah/>')

    expect:
    !matcher.matchBody(expected(), actual(), false).empty
  }

  def 'matching XML bodies - returns a mismatch - when the root elements do not match'() {
    given:
    actualBody = OptionalBody.body('<bar></bar>')
    expectedBody = OptionalBody.body('<foo/>')

    when:
    def mismatches = matcher.matchBody(expected(), actual(), false)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected element foo but received bar']
    mismatches*.path == ['$.foo']
  }

  def 'matching XML bodies - returns a mismatch - when comparing an empty list to a non-empty one'() {
    given:
    actualBody = OptionalBody.body('<foo><item/></foo>')
    expectedBody = OptionalBody.body('<foo></foo>')

    when:
    def mismatches = matcher.matchBody(expected(), actual(), false)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected an empty List but received <item/>']
    mismatches*.path == ['$.foo']
  }

  def 'matching XML bodies - returns a mismatch - when comparing a list to one with with different size'() {
    given:
    actualBody = OptionalBody.body('<foo><one/><two/><three/></foo>')
    expectedBody = OptionalBody.body('<foo><one/><two/><three/><four/></foo>')

    when:
    def mismatches = matcher.matchBody(expected(), actual(), false)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected <four/> but was missing', 'Expected a List with 4 elements but received 3 elements']
    mismatches*.path.unique() == ['$.foo']
  }

  def 'matching XML bodies - returns a mismatch - when comparing a list to one with with the same size but different children'() {
    given:
    actualBody = OptionalBody.body('<foo><one/><two/><four/></foo>')
    expectedBody = OptionalBody.body('<foo><one/><two/><three/></foo>')

    when:
    def mismatches = matcher.matchBody(expected(), actual(), false)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected element three but received four']
    mismatches*.path.unique() == ['$.foo.2.three']
  }

  def 'matching XML bodies - returns a mismatch - when comparing a list to one where the items are in the wrong order'() {
    given:
    actualBody = OptionalBody.body('<foo><one/><three/><two/></foo>')
    expectedBody = OptionalBody.body('<foo><one/><two/><three/></foo>')

    when:
    def mismatches = matcher.matchBody(expected(), actual(), false)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected element two but received three', 'Expected element three but received two']
  }

  def 'matching XML bodies - returns a mismatch - when comparing a tags attributes to one with less entries'() {
    given:
    actualBody = OptionalBody.body('<foo something="100"/>')
    expectedBody = OptionalBody.body('<foo something="100" somethingElse="101"/>')

    when:
    def mismatches = matcher.matchBody(expected(), actual(), false)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected a Tag with 2 attributes but received 1 attributes',
                             'Expected somethingElse=\'101\' but was missing']
  }

  def 'matching XML bodies - returns a mismatch - when comparing a tags attributes to one with more entries'() {
    given:
    actualBody = OptionalBody.body('<foo something="100" somethingElse="101"/>')
    expectedBody = OptionalBody.body('<foo something="100"/>')

    when:
    def mismatches = matcher.matchBody(expected(), actual(), false)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected a Tag with 1 attributes but received 2 attributes']
  }

  def 'matching XML bodies - returns a mismatch - when a tag is missing an attribute'() {
    given:
    actualBody = OptionalBody.body('<foo something="100"/>')
    expectedBody = OptionalBody.body('<foo something="100" somethingElse="100"/>')

    when:
    def mismatches = matcher.matchBody(expected(), actual(), false)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected a Tag with 2 attributes but received 1 attributes',
                             'Expected somethingElse=\'100\' but was missing']
  }

  def 'matching XML bodies - returns a mismatch - when a tag has the same number of attributes but different keys'() {
    given:
    actualBody = OptionalBody.body('<foo something="100" somethingDifferent="100"/>')
    expectedBody = OptionalBody.body('<foo something="100" somethingElse="100"/>')

    when:
    def mismatches = matcher.matchBody(expected(), actual(), false)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected somethingElse=\'100\' but was missing']
    mismatches*.path == ['$.foo.@somethingElse']
  }

  def 'matching XML bodies - returns a mismatch - when a tag has an invalid value'() {
    given:
    actualBody = OptionalBody.body('<foo something="101"/>')
    expectedBody = OptionalBody.body('<foo something="100"/>')

    when:
    def mismatches = matcher.matchBody(expected(), actual(), false)

    then:
    !mismatches.empty
    mismatches*.mismatch == ["Expected something='100' but received 101"]
    mismatches*.path == ['$.foo.@something']
  }

  def 'matching XML bodies - returns a mismatch - when the content of an element does not match'() {
    given:
    actualBody = OptionalBody.body('<foo>hello my friend</foo>')
    expectedBody = OptionalBody.body('<foo>hello world</foo>')

    when:
    def mismatches = matcher.matchBody(expected(), actual(), false)

    then:
    !mismatches.empty
    mismatches*.mismatch == ["Expected value 'hello world' but received 'hello my friend'"]
    mismatches*.path == ['$.foo.#text']
  }

  def 'matching XML bodies - with a matcher defined - delegate to the matcher'() {
    given:
    actualBody = OptionalBody.body('<foo something="101"/>')
    expectedBody = OptionalBody.body('<foo something="100"/>')
    matchers.addCategory('body').addRule("\$.foo['@something']", new RegexMatcher('\\d+'))

    expect:
    matcher.matchBody(expected(), actual(), false).empty
  }

}
