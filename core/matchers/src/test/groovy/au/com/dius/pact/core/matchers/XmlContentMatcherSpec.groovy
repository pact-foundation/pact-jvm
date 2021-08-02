package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

@SuppressWarnings(['LineLength', 'PrivateFieldCouldBeFinal'])
class XmlContentMatcherSpec extends Specification {

  private OptionalBody expectedBody, actualBody
  private XmlContentMatcher matcher
  private MatchingContext context
  private MatchingContext noUnexpectedKeysContext

  def setup() {
    System.clearProperty('pact.matching.xml.namespace-aware')

    matcher = new XmlContentMatcher()
    expectedBody = OptionalBody.missing()
    actualBody = OptionalBody.missing()
    context = new MatchingContext(new MatchingRuleCategory('body'), true)
    noUnexpectedKeysContext = new MatchingContext(new MatchingRuleCategory('body'), false)
  }

  def 'matching XML bodies - when comparing missing bodies'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
  }

  def 'matching XML bodies - when comparing empty bodies'() {
    given:
    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.empty()

    expect:
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
  }

  def 'matching XML bodies - when comparing a missing body to anything'() {
    given:
    actualBody = OptionalBody.body('Blah'.bytes)
    expectedBody = OptionalBody.missing()

    expect:
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
  }

  def 'matching XML bodies - with equal bodies'() {
    given:
    actualBody = OptionalBody.body('<blah/>'.bytes)
    expectedBody = OptionalBody.body('<blah/>'.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
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
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
  }

  def 'matching XML bodies - when allowUnexpectedKeys is true - and comparing an empty list to a non-empty one'() {
    given:
    actualBody = OptionalBody.body('<foo><item/></foo>'.bytes)
    expectedBody = OptionalBody.body('<foo></foo>'.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty
  }

  def 'matching XML bodies - when allowUnexpectedKeys is true - and comparing a list to a super-set'() {
    given:
    actualBody = OptionalBody.body('<foo><item1/><item2/></foo>'.bytes)
    expectedBody = OptionalBody.body('<foo><item1/></foo>'.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty
  }

  def 'matching XML bodies - when allowUnexpectedKeys is true - and comparing a tags attributes to one with more entries'() {
    given:
    actualBody = OptionalBody.body('<foo something="100" somethingElse="101"/>'.bytes)
    expectedBody = OptionalBody.body('<foo something="100"/>'.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty
  }

  def 'matching XML bodies - returns a mismatch - when comparing anything to an empty body'() {
    given:
    expectedBody = OptionalBody.body('<blah/>'.bytes)

    expect:
    !matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
  }

  def 'matching XML bodies - returns a mismatch - when the root elements do not match'() {
    given:
    actualBody = OptionalBody.body('<bar></bar>'.bytes)
    expectedBody = OptionalBody.body('<foo/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

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
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected an empty List but received 1 child nodes', 'Unexpected child <item/>']
    mismatches*.path == ['$.foo', '$.foo']
  }

  def 'matching XML bodies - returns a mismatch - when comparing a list to one with with different size'() {
    given:
    actualBody = OptionalBody.body('<foo><one/><two/><three/></foo>'.bytes)
    expectedBody = OptionalBody.body('<foo><one/><two/><three/><four/></foo>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

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
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected child <three/> but was missing', 'Unexpected child <four/>']
    mismatches*.path == ['$.foo.three.1', '$.foo']
  }

  def 'matching XML bodies - returns no mismatch - when comparing a list to one where the items are in the wrong order'() {
    given:
    actualBody = OptionalBody.body('<foo><one/><three/><two/></foo>'.bytes)
    expectedBody = OptionalBody.body('<foo><one/><two/><three/></foo>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

    then:
    mismatches.empty
  }

  def 'matching XML bodies - returns a mismatch - when comparing a tags attributes to one with less entries'() {
    given:
    actualBody = OptionalBody.body('<foo something="100"/>'.bytes)
    expectedBody = OptionalBody.body('<foo something="100" somethingElse="101"/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

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
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected a Tag with 1 attributes but received 2 attributes']
  }

  def 'matching XML bodies - returns a mismatch - when a tag is missing an attribute'() {
    given:
    actualBody = OptionalBody.body('<foo something="100"/>'.bytes)
    expectedBody = OptionalBody.body('<foo something="100" somethingElse="100"/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

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
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

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
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

    then:
    !mismatches.empty
    mismatches*.mismatch == ["Expected something='100' but received something='101'"]
    mismatches*.path == ['$.foo.@something']
  }

  def 'matching XML bodies - returns a mismatch - when the content of an element does not match'() {
    given:
    actualBody = OptionalBody.body('<foo>hello my friend</foo>'.bytes)
    expectedBody = OptionalBody.body('<foo>hello world</foo>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

    then:
    !mismatches.empty
    mismatches*.mismatch == ["Expected value 'hello world' but received 'hello my friend'"]
    mismatches*.path == ['$.foo.#text']
  }

  def 'matching XML bodies - with a matcher defined - delegate to the matcher'() {
    given:
    actualBody = OptionalBody.body('<foo something="101"/>'.bytes)
    expectedBody = OptionalBody.body('<foo something="100"/>'.bytes)
    noUnexpectedKeysContext.matchers.addRule("\$.foo['@something']", new RegexMatcher('\\d+'))

    expect:
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
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
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty
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
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
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

    noUnexpectedKeysContext.matchers.addRule('$.providerService.attribute1.newattribute2.hiddenData',
      new RegexMatcher('[a-zA-Z0-9]*'))

    expect:
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
  }

  @Unroll
  def 'matching XML bodies - with different namespace declarations'() {
    given:
    actualBody = OptionalBody.body(actual.bytes)
    expectedBody = OptionalBody.body(expected.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty

    where:
    actual                       | expected
    '<blah xmlns="urn:ns"/>'     | '<blah xmlns="urn:ns"/>'
    '<blah xmlns="urn:ns"/>'     | '<b:blah xmlns:b="urn:ns"/>'
    '<a:blah xmlns:a="urn:ns"/>' | '<blah xmlns="urn:ns"/>'
    '<a:blah xmlns:a="urn:ns"/>' | '<b:blah xmlns:b="urn:ns"/>'
  }

  @Unroll
  def 'matching XML bodies - with different namespace declarations - and have child elements'() {
    given:
    actualBody = OptionalBody.body(actual.bytes)
    expectedBody = OptionalBody.body(expected.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty

    where:
    actual                                                         | expected
    '<foo xmlns="urn:ns"><item/></foo>'                            | '<foo xmlns="urn:ns"><item/></foo>'
    '<foo xmlns="urn:ns"><item/></foo>'                            | '<b:foo xmlns:b="urn:ns"><b:item/></b:foo>'
    '<a:foo xmlns:a="urn:ns"><a:item/></a:foo>'                    | '<foo xmlns="urn:ns"><item/></foo>'
    '<a:foo xmlns:a="urn:ns"><a:item/></a:foo>'                    | '<b:foo xmlns:b="urn:ns"><b:item/></b:foo>'
    '<a:foo xmlns:a="urn:ns"><a2:item xmlns:a2="urn:ns"/></a:foo>' | '<b:foo xmlns:b="urn:ns"><b:item/></b:foo>'
  }

  def 'matching XML bodies - returns a mismatch - when different namespaces are used'() {
    given:
    actualBody = OptionalBody.body('<blah xmlns="urn:other"/>'.bytes)
    expectedBody = OptionalBody.body('<blah xmlns="urn:ns"/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected element {urn:ns}blah but received {urn:other}blah']
    mismatches*.path == ['$.blah']
  }

  def 'matching XML bodies - returns a mismatch - when expected namespace is not used'() {
    given:
    actualBody = OptionalBody.body('<blah/>'.bytes)
    expectedBody = OptionalBody.body('<blah xmlns="urn:ns"/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected element {urn:ns}blah but received blah']
    mismatches*.path == ['$.blah']
  }

  def 'matching XML bodies - returns a mismatch - when allowUnexpectedKeys is true - and no namespace is expected'() {
    given:
    actualBody = OptionalBody.body('<blah xmlns="urn:ns"/>'.bytes)
    expectedBody = OptionalBody.body('<blah/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, context).mismatches

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected element blah but received {urn:ns}blah']
    mismatches*.path == ['$.blah']
  }

  @RestoreSystemProperties
  def 'matching XML bodies - when allowUnexpectedKeys is true - and namespace-aware matching disabled - and no namespace is expected'() {
    given:
    System.setProperty('pact.matching.xml.namespace-aware', 'false')
    actualBody = OptionalBody.body('<blah xmlns="urn:ns"/>'.bytes)
    expectedBody = OptionalBody.body('<blah/>'.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty
  }

  def 'matching XML bodies - when attribute uses different prefix'() {
    given:
    actualBody = OptionalBody.body('<foo xmlns:a="urn:ns" a:something="100"/>'.bytes)
    expectedBody = OptionalBody.body('<foo xmlns:b="urn:ns" b:something="100"/>'.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty
  }

  def 'matching XML bodies - returns a mismatch - when attribute uses different namespace'() {
    given:
    actualBody = OptionalBody.body('<foo xmlns:ns="urn:a" ns:something="100"/>'.bytes)
    expectedBody = OptionalBody.body('<foo xmlns:ns="urn:b" ns:something="100"/>'.bytes)

    when:
    def mismatches = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected {urn:b}something=\'100\' but was missing']
    mismatches*.path == ['$.foo.@ns:something']
  }

  def 'matching XML bodies - with namespaces and a matcher defined - delegate to matcher for attribute'() {
    given:
    actualBody = OptionalBody.body('<foo xmlns:a="urn:ns" a:something="100"/>'.bytes)
    expectedBody = OptionalBody.body('<foo xmlns:b="urn:ns" b:something="101"/>'.bytes)
    noUnexpectedKeysContext.matchers.addRule("\$.foo['@b:something']", new RegexMatcher('\\d+'))

    expect:
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
  }

  def 'matching XML bodies - with namespaces and a matcher defined - delegate to the matcher'() {
    given:
    actualBody = OptionalBody.body('<ns:foo xmlns:ns="urn:ns"><ns:something>100</ns:something></ns:foo>'.bytes)
    expectedBody = OptionalBody.body('<foo xmlns="urn:ns"><something>101</something></foo>'.bytes)
    noUnexpectedKeysContext.matchers.addRule("\$.foo.something", new RegexMatcher('\\d+'))

    expect:
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
  }

  def 'when an element has different types of children but we allow unexpected keys'() {
    given:
    def actual = '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
      <animals>
        <dog id="1" name="Canine"/>
        <dog id="1" name="Canine"/>
        <cat id="2" name="Feline"/>
        <cat id="2" name="Feline"/>
        <cat id="2" name="Feline"/>
        <wolf id="3" name="Canine"/>
      </animals>
    '''
    actualBody = OptionalBody.body(actual.bytes)

    def expected = '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
      <animals>
        <dog id="1" name="Canine"/>
        <cat id="2" name="Feline"/>
        <wolf id="3" name="Canine"/>
      </animals>
    '''
    expectedBody = OptionalBody.body(expected.bytes)

    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty
  }

  def 'when an element has different types of children but we do not allow unexpected keys'() {
    given:
    def actual = '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
      <animals>
        <dog id="1" name="Canine"/>
        <dog id="1" name="Canine"/>
        <cat id="2" name="Feline"/>
        <cat id="2" name="Feline"/>
        <cat id="2" name="Feline"/>
        <wolf id="3" name="Canine"/>
      </animals>
    '''
    actualBody = OptionalBody.body(actual.bytes)

    def expected = '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
      <animals>
        <dog id="1" name="Canine"/>
        <cat id="2" name="Feline"/>
        <wolf id="3" name="Canine"/>
      </animals>
    '''
    expectedBody = OptionalBody.body(expected.bytes)

    when:
    def result = matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches

    then:
    result.size() == 3
    result*.description() == ['Unexpected child <dog/>',
                              'Unexpected child <cat/>',
                              'Unexpected child <cat/>']
  }

  def 'type matcher when an element has different types of children'() {
    given:
    def actual = '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
      <animals>
        <dog id="1" name="Canine"/>
        <cat id="2" name="Feline"/>
        <dog id="1" name="Canine"/>
        <cat id="2" name="Feline"/>
        <cat id="2" name="Feline"/>
        <wolf id="3" name="Canine"/>
      </animals>
    '''
    actualBody = OptionalBody.body(actual.bytes)

    def expected = '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
      <animals>
        <dog id="1" name="Canine"/>
        <cat id="2" name="Feline"/>
        <wolf id="3" name="Canine"/>
      </animals>
    '''
    expectedBody = OptionalBody.body(expected.bytes)
    noUnexpectedKeysContext.matchers
     .addRule("\$.animals.*", TypeMatcher.INSTANCE)
     .addRule("\$.animals.*['@id']", new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))

    expect:
    matcher.matchBody(expectedBody, actualBody, noUnexpectedKeysContext).mismatches.empty
  }
}
