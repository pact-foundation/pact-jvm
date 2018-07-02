package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.Feature
import au.com.dius.pact.core.model.FeatureToggles
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.matchingrules.ValuesMatcher
import spock.lang.Specification
import spock.lang.Unroll

class PactDslJsonBodySpec extends Specification {

  def 'close must close off all parents and return the root'() {
    given:
      def root = new PactDslJsonBody()
      def array = new PactDslJsonArray('b', '', root)
      def obj = new PactDslJsonBody('c', '', array)

    when:
      def result = obj.close()

    then:
      root.closed
      obj.closed
      array.closed
      result.is root
  }

  @Unroll
  def 'min array like function should set the example size to the min size'() {
    expect:
    obj.close().body.getJSONArray('test').length() == 2

    where:
    obj << [
      new PactDslJsonBody().minArrayLike('test', 2).id(),
      new PactDslJsonBody().minArrayLike('test', 2, PactDslJsonRootValue.id()),
      new PactDslJsonBody().minMaxArrayLike('test', 2, 3).id(),
    ]
  }

  def 'min array like function should validate the number of examples match the min size'() {
    when:
    new PactDslJsonBody().minArrayLike('test', 3, 2)

    then:
    thrown(IllegalArgumentException)
  }

  def 'min array like function with root value should validate the number of examples match the min size'() {
    when:
    new PactDslJsonBody().minArrayLike('test', 3, PactDslJsonRootValue.id(), 2)

    then:
    thrown(IllegalArgumentException)
  }

  def 'max array like function should validate the number of examples match the max size'() {
    when:
    new PactDslJsonBody().maxArrayLike('test', 3, 4)

    then:
    thrown(IllegalArgumentException)
  }

  def 'max array like function with root value should validate the number of examples match the max size'() {
    when:
    new PactDslJsonBody().minArrayLike('test', 4, PactDslJsonRootValue.id(), 3)

    then:
    thrown(IllegalArgumentException)
  }

  def 'minMax array like function should validate the number of examples match the min size'() {
    when:
    new PactDslJsonBody().minMaxArrayLike('test', 3, 4, 2)

    then:
    thrown(IllegalArgumentException)
  }

  def 'minMax array like function with root value should validate the number of examples match the min size'() {
    when:
    new PactDslJsonBody().minMaxArrayLike('test', 3, 4, PactDslJsonRootValue.id(), 2)

    then:
    thrown(IllegalArgumentException)
  }

  def 'minmax array like function should validate the number of examples match the max size'() {
    when:
    new PactDslJsonBody().minMaxArrayLike('test', 2, 3, 4)

    then:
    thrown(IllegalArgumentException)
  }

  def 'minmax array like function with root value should validate the number of examples match the max size'() {
    when:
    new PactDslJsonBody().minMaxArrayLike('test', 2, 3, PactDslJsonRootValue.id(), 4)

    then:
    thrown(IllegalArgumentException)
  }

  def 'each array with max like function should validate the number of examples match the max size'() {
    when:
    new PactDslJsonBody().eachArrayWithMaxLike('test', 4, 3)

    then:
    thrown(IllegalArgumentException)
  }

  def 'each array with min function should validate the number of examples match the min size'() {
    when:
    new PactDslJsonBody().eachArrayWithMinLike('test', 2, 3)

    then:
    thrown(IllegalArgumentException)
  }

  def 'each array with minmax like function should validate the number of examples match the max size'() {
    when:
    new PactDslJsonBody().eachArrayWithMinMaxLike('test', 4, 2, 3)

    then:
    thrown(IllegalArgumentException)
  }

  def 'each array with minmax function should validate the number of examples match the min size'() {
    when:
    new PactDslJsonBody().eachArrayWithMinMaxLike('test', 1, 2, 3)

    then:
    thrown(IllegalArgumentException)
  }

  def 'with nested objects, the rule logic value should be copied'() {
    expect:
    body.matchers.matchingRules['.foo.bar'].ruleLogic == RuleLogic.OR

    where:
    body = new PactDslJsonBody().object('foo')
      .or('bar', 42, PM.numberType(), PM.nullValue())
      .closeObject()
  }

  def 'generate the correct JSON when the attribute name is a number'() {
    expect:
    new PactDslJsonBody()
      .stringType('asdf')
      .array('0').closeArray()
      .eachArrayLike('1').closeArray().closeArray()
      .eachArrayWithMaxLike('2', 10).closeArray().closeArray()
      .eachArrayWithMinLike('3', 10).closeArray().closeArray()
            .close().toString() == '{"0":[],"1":[[]],"2":[[]],"3":[[],[],[],[],[],[],[],[],[],[]],"asdf":"string"}'
  }

  def 'generate the correct JSON when the attribute name has a space'() {
    expect:
    new PactDslJsonBody()
      .array('available Options')
        .object()
        .stringType('Material', 'Gold')
      . closeObject()
      .closeArray().toString() == '{"available Options":[{"Material":"Gold"}]}'
  }

  def 'test for behaviour of close for issue 619'() {
    given:
    PactDslJsonBody pactDslJsonBody = new PactDslJsonBody()
    PactDslJsonBody contactDetailsPactDslJsonBody = pactDslJsonBody.object('contactDetails')
    contactDetailsPactDslJsonBody.object('mobile')
      .stringType('countryCode', '64')
      .stringType('prefix', '21')
      .stringType('subscriberNumber', '123456')
      .closeObject()
    pactDslJsonBody = contactDetailsPactDslJsonBody.closeObject().close()

    expect:
    pactDslJsonBody.close().matchers.toMap(PactSpecVersion.V2) == [
      '$.body.contactDetails.mobile.countryCode': [match: 'type'],
      '$.body.contactDetails.mobile.prefix': [match: 'type'],
      '$.body.contactDetails.mobile.subscriberNumber': [match: 'type']
    ]
  }

  def 'test for behaviour of close for issue 628'() {
    given:
    PactDslJsonBody getBody = new PactDslJsonBody()
    getBody
      .object('metadata')
      .stringType('messageId', 'test')
      .stringType('date', 'test')
      .stringType('contractVersion', 'test')
      .closeObject()
      .object('payload')
      .stringType('name', 'srm.countries.get')
      .stringType('iri', 'some_iri')
      .closeObject()
      .closeObject()

    expect:
    getBody.close().matchers.toMap(PactSpecVersion.V2) == [
      '$.body.metadata.messageId': [match: 'type'],
      '$.body.metadata.date': [match: 'type'],
      '$.body.metadata.contractVersion': [match: 'type'],
      '$.body.payload.name': [match: 'type'],
      '$.body.payload.iri': [match: 'type']
    ]
  }

  def 'eachKey - generate a wildcard matcher pattern if useMatchValuesMatcher is not set'() {
    given:
    FeatureToggles.toggleFeature(Feature.UseMatchValuesMatcher, false)

    def pactDslJsonBody = new PactDslJsonBody()
      .object('one')
        .eachKeyLike('key1')
          .id()
          .closeObject()
      .closeObject()
      .object('two')
        .eachKeyLike('key2', PactDslJsonRootValue.stringMatcher('\\w+', 'test'))
      .closeObject()
      .object('three')
        .eachKeyMappedToAnArrayLike('key3')
          .id('key3-id')
          .closeObject()
        .closeArray()
      .closeObject()

    when:
    pactDslJsonBody.close()

    then:
    pactDslJsonBody.matchers.matchingRules == [
      '$.one.*': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.one.*.id': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.two.*': new MatchingRuleGroup([new RegexMatcher('\\w+')]),
      '$.three.*': new MatchingRuleGroup([new MinTypeMatcher(0)]),
      '$.three.*[*].key3-id': new MatchingRuleGroup([TypeMatcher.INSTANCE])
    ]

    cleanup:
    FeatureToggles.reset()
  }

  def 'eachKey - generate a match values matcher if useMatchValuesMatcher is set'() {
    given:
    FeatureToggles.toggleFeature(Feature.UseMatchValuesMatcher, true)

    def pactDslJsonBody = new PactDslJsonBody()
      .object('one')
      .eachKeyLike('key1')
      .id()
      .closeObject()
      .closeObject()
      .object('two')
      .eachKeyLike('key2', PactDslJsonRootValue.stringMatcher('\\w+', 'test'))
      .closeObject()
      .object('three')
      .eachKeyMappedToAnArrayLike('key3')
      .id('key3-id')
      .closeObject()
      .closeArray()
      .closeObject()

    when:
    pactDslJsonBody.close()

    then:
    pactDslJsonBody.matchers.matchingRules == [
      '$.one': new MatchingRuleGroup([ValuesMatcher.INSTANCE]),
      '$.one.*.id': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.two': new MatchingRuleGroup([ValuesMatcher.INSTANCE]),
      '$.two.*': new MatchingRuleGroup([new RegexMatcher('\\w+')]),
      '$.three': new MatchingRuleGroup([ValuesMatcher.INSTANCE]),
      '$.three.*[*].key3-id': new MatchingRuleGroup([TypeMatcher.INSTANCE])
    ]

    cleanup:
    FeatureToggles.reset()
  }

  def 'Allow an attribute to be defined from a DSL part'() {
    given:
    PactDslJsonBody contactDetailsPactDslJsonBody = new PactDslJsonBody()
    contactDetailsPactDslJsonBody.object('mobile')
      .stringType('countryCode', '64')
      .stringType('prefix', '21')
      .numberType('subscriberNumber')
      .closeObject()
    PactDslJsonBody pactDslJsonBody = new PactDslJsonBody()
      .object('contactDetails', contactDetailsPactDslJsonBody)
      .object('contactDetails2', contactDetailsPactDslJsonBody)
      .close()

    expect:
    pactDslJsonBody.matchers.toMap(PactSpecVersion.V2) == [
      '$.body.contactDetails.mobile.countryCode': [match: 'type'],
      '$.body.contactDetails.mobile.prefix': [match: 'type'],
      '$.body.contactDetails.mobile.subscriberNumber': [match: 'number'],
      '$.body.contactDetails2.mobile.countryCode': [match: 'type'],
      '$.body.contactDetails2.mobile.prefix': [match: 'type'],
      '$.body.contactDetails2.mobile.subscriberNumber': [match: 'number']
    ]
    pactDslJsonBody.generators.toMap(PactSpecVersion.V3) == [
      body: [
        '$.contactDetails.mobile.subscriberNumber': [type: 'RandomInt', min: 0, max: 2147483647],
        '$.contactDetails2.mobile.subscriberNumber': [type: 'RandomInt', min: 0, max: 2147483647]
      ]
    ]
    pactDslJsonBody.toString() == '{"contactDetails2":{"mobile":{"countryCode":"64","prefix":"21","subscriberNumber":' +
      '100}},"contactDetails":{"mobile":{"countryCode":"64","prefix":"21","subscriberNumber":100}}}'
  }

}
