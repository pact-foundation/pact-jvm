package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import spock.lang.Issue
import spock.lang.Specification

@SuppressWarnings(['LineLength', 'PrivateFieldCouldBeFinal'])
class XmlBodyMatcherSpec extends Specification {

  private OptionalBody expectedBody, actualBody
  private MatchingRulesImpl matchers
  private XmlBodyMatcher matcher

  def setup() {
    matcher = new XmlBodyMatcher()
    matchers = new MatchingRulesImpl()
    expectedBody = OptionalBody.missing()
    actualBody = OptionalBody.missing()
  }

  def 'matching XML bodies - when comparing missing bodies'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, false, matchers).empty
  }

  def 'matching XML bodies - when comparing empty bodies'() {
    given:
    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.empty()

    expect:
    matcher.matchBody(expectedBody, actualBody, false, matchers).empty
  }

  def 'matching XML bodies - when comparing a missing body to anything'() {
    given:
    actualBody = OptionalBody.body('Blah'.bytes)
    expectedBody = OptionalBody.missing()

    expect:
    matcher.matchBody(expectedBody, actualBody, false, matchers).empty
  }

  def 'matching XML bodies - with equal bodies'() {
    given:
    actualBody = OptionalBody.body('<blah/>'.bytes)
    expectedBody = OptionalBody.body('<blah/>'.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, false, matchers).empty
  }

  def 'matching XML bodies - when bodies differ only in whitespace'() {
    given:
    actualBody = OptionalBody.body(
          '''<foo>
            |  <bar></bar>
            |</foo>
          '''.stripMargin().bytes)
    expectedBody = OptionalBody.body('<foo><bar></bar></foo>'.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, false, matchers).empty
  }

  def 'matching XML bodies - when allowUnexpectedKeys is true - and comparing an empty list to a non-empty one'() {
    given:
    actualBody = OptionalBody.body('<foo><item/></foo>'.bytes)
    expectedBody = OptionalBody.body('<foo></foo>'.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty
  }

  def 'matching XML bodies - when allowUnexpectedKeys is true - and comparing a list to a super-set'() {
    given:
    actualBody = OptionalBody.body('<foo><item1/><item2/></foo>'.bytes)
    expectedBody = OptionalBody.body('<foo><item1/></foo>'.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty
  }

  def 'matching XML bodies - when allowUnexpectedKeys is true - and comparing a tags attributes to one with more entries'() {
    given:
    actualBody = OptionalBody.body('<foo something="100" somethingElse="101"/>'.bytes)
    expectedBody = OptionalBody.body('<foo something="100"/>'.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, true, matchers).empty
  }

  def 'matching XML bodies - returns a mismatch - when comparing anything to an empty body'() {
    given:
    expectedBody = OptionalBody.body('<blah/>'.bytes)

    expect:
    !matcher.matchBody(expectedBody, actualBody, false, matchers).empty
  }

  def 'matching XML bodies - returns a mismatch - when the root elements do not match'() {
    given:
    actualBody = OptionalBody.body('<bar></bar>'.bytes)
    expectedBody = OptionalBody.body('<foo/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, false, matchers)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected element foo but received bar']
    mismatches*.path == ['$.foo']
  }

  def 'matching XML bodies - returns a mismatch - when comparing an empty list to a non-empty one'() {
    given:
    actualBody = OptionalBody.body('<foo><item/></foo>'.bytes)
    expectedBody = OptionalBody.body('<foo></foo>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, false, matchers)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected an empty List but received 1 child nodes']
    mismatches*.path == ['$.foo']
  }

  def 'matching XML bodies - returns a mismatch - when comparing a list to one with with different size'() {
    given:
    actualBody = OptionalBody.body('<foo><one/><two/><three/></foo>'.bytes)
    expectedBody = OptionalBody.body('<foo><one/><two/><three/><four/></foo>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, false, matchers)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected child <four/> but was missing']
    mismatches*.path == ['$.foo']
  }

  def 'matching XML bodies - returns a mismatch - when comparing a list to one with with the same size but different children'() {
    given:
    actualBody = OptionalBody.body('<foo><one/><two/><three/><four/></foo>'.bytes)
    expectedBody = OptionalBody.body('<foo><one/><two/><three/><three/></foo>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, false, matchers)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected child <three/> but was missing']
    mismatches*.path == ['$.foo.three.1']
  }

  def 'matching XML bodies - returns no mismatch - when comparing a list to one where the items are in the wrong order'() {
    given:
    actualBody = OptionalBody.body('<foo><one/><three/><two/></foo>'.bytes)
    expectedBody = OptionalBody.body('<foo><one/><two/><three/></foo>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, false, matchers)

    then:
    mismatches.empty
  }

  def 'matching XML bodies - returns a mismatch - when comparing a tags attributes to one with less entries'() {
    given:
    actualBody = OptionalBody.body('<foo something="100"/>'.bytes)
    expectedBody = OptionalBody.body('<foo something="100" somethingElse="101"/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, false, matchers)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected a Tag with at least 2 attributes but received 1 attributes',
                             'Expected somethingElse=\'101\' but was missing']
  }

  def 'matching XML bodies - returns a mismatch - when comparing a tags attributes to one with more entries'() {
    given:
    actualBody = OptionalBody.body('<foo something="100" somethingElse="101"/>'.bytes)
    expectedBody = OptionalBody.body('<foo something="100"/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, false, matchers)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected a Tag with 1 attributes but received 2 attributes']
  }

  def 'matching XML bodies - returns a mismatch - when a tag is missing an attribute'() {
    given:
    actualBody = OptionalBody.body('<foo something="100"/>'.bytes)
    expectedBody = OptionalBody.body('<foo something="100" somethingElse="100"/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, false, matchers)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected a Tag with at least 2 attributes but received 1 attributes',
                             'Expected somethingElse=\'100\' but was missing']
  }

  def 'matching XML bodies - returns a mismatch - when a tag has the same number of attributes but different keys'() {
    given:
    actualBody = OptionalBody.body('<foo something="100" somethingDifferent="100"/>'.bytes)
    expectedBody = OptionalBody.body('<foo something="100" somethingElse="100"/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, false, matchers)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected somethingElse=\'100\' but was missing']
    mismatches*.path == ['$.foo.@somethingElse']
  }

  def 'matching XML bodies - returns a mismatch - when a tag has an invalid value'() {
    given:
    actualBody = OptionalBody.body('<foo something="101"/>'.bytes)
    expectedBody = OptionalBody.body('<foo something="100"/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, false, matchers)

    then:
    !mismatches.empty
    mismatches*.mismatch == ["Expected something='100' but received 101"]
    mismatches*.path == ['$.foo.@something']
  }

  def 'matching XML bodies - returns a mismatch - when the content of an element does not match'() {
    given:
    actualBody = OptionalBody.body('<foo>hello my friend</foo>'.bytes)
    expectedBody = OptionalBody.body('<foo>hello world</foo>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, false, matchers)

    then:
    !mismatches.empty
    mismatches*.mismatch == ["Expected value 'hello world' but received 'hello my friend'"]
    mismatches*.path == ['$.foo.#text']
  }

  def 'matching XML bodies - with a matcher defined - delegate to the matcher'() {
    given:
    actualBody = OptionalBody.body('<foo something="101"/>'.bytes)
    expectedBody = OptionalBody.body('<foo something="100"/>'.bytes)
    matchers.addCategory('body').addRule("\$.foo['@something']", new RegexMatcher('\\d+'))

    expect:
    matcher.matchBody(expectedBody, actualBody, false, matchers).empty
  }

  @Issue('#899')
  def 'matching XML bodies - with unexpected elements'() {
    given:
    actualBody = OptionalBody.body(('<note> <to>John</to> <from>Jane</from> <subject>Reminder</subject> ' +
      '<address> <firstName>John</firstName> <lastName>Doe</lastName> <street>Prince Street</street> ' +
      '<number>34</number> <city>Manchester</city>\t</address> </note>').bytes)
    expectedBody = OptionalBody.body(('<note> <to>John</to> <from>Jane</from> <subject>Reminder</subject> ' +
      '<address> <city>Manchester</city>\t</address> </note>').bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, false, matchers).empty
  }

  @Issue('#975')
  def 'matching XML bodies - with CDATA elements'() {
    given:
    def xml = '''<?xml version="1.0" encoding="UTF-8"?>
     <providerService version="1.0">
       <attribute1>
         <newattribute>
             <date month="11" year="2019"/>
           <name><![CDATA[Surname Name]]></name>
         </newattribute>
         <newattribute2>
           <countryCode>RO</countryCode>
           <hiddenData>ABCD***************010101</hiddenData>
         </newattribute2>
       </attribute1>
     </providerService>
    '''
    actualBody = OptionalBody.body(xml.bytes)
    expectedBody = OptionalBody.body(xml.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, false, matchers).empty
  }

  @Issue('#975')
  def 'matching XML bodies - with CDATA elements matching with regex'() {
    given:
    def expected = '''<?xml version="1.0" encoding="UTF-8"?>
     <providerService version="1.0">
       <attribute1>
         <newattribute>
             <date month="11" year="2019"/>
           <name><![CDATA[Surname Name]]></name>
         </newattribute>
         <newattribute2>
           <countryCode>RO</countryCode>
           <hiddenData>OWY0NzEyYTAyMmMzZjI2Y2RmYzZiMTcx</hiddenData>
         </newattribute2>
       </attribute1>
     </providerService>
    '''
    def actual = '''<?xml version="1.0" encoding="UTF-8"?>
     <providerService version="1.0">
       <attribute1>
         <newattribute>
             <date month="11" year="2019"/>
           <name><![CDATA[Surname Name]]></name>
         </newattribute>
         <newattribute2>
           <countryCode>RO</countryCode>
           <hiddenData><![CDATA[Mjc5MGJkNDVjZTI3OWNjYjJjMmYzZTVh]]></hiddenData>
         </newattribute2>
       </attribute1>
     </providerService>
    '''

    actualBody = OptionalBody.body(actual.bytes)
    expectedBody = OptionalBody.body(expected.bytes)

    matchers.addCategory('body').addRule('$.providerService.attribute1.newattribute2.hiddenData',
      new RegexMatcher('[a-zA-Z0-9]*'))

    expect:
    matcher.matchBody(expectedBody, actualBody, false, matchers).empty
  }

}
