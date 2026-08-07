package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import com.google.protobuf.Struct
import com.google.protobuf.Value
import io.pact.plugin.v2.PluginV2
import io.pact.plugins.jvm.core.FieldContext
import io.pact.plugins.jvm.core.FieldTestMode
import io.pact.plugins.jvm.core.FieldValue
import io.pact.plugins.jvm.core.FieldKt
import spock.lang.Specification

/**
 * The host side of proposal 009: a plugin calling back for one of this framework's standard rules
 * or generators, applied to a single value.
 */
class CoreFieldCapabilitiesSpec extends Specification {

  private static PluginV2.MatchFieldRequest matchRequest(
    String key, String ruleType, Map<String, Object> ruleValues, FieldValue expected, FieldValue actual) {
    def builder = PluginV2.MatchFieldRequest.newBuilder()
      .setKey(key)
      .setPath('$.one')
      .setMismatchType('body')
      .setExpected(expected.toProto())
      .setActual(actual.toProto())
    if (ruleType != null) {
      builder.setRule(PluginV2.MatchingRule.newBuilder()
        .setType(ruleType)
        .setValues(toStruct(ruleValues)))
    }
    builder.build()
  }

  private static PluginV2.GenerateFieldRequest generateRequest(
    String key, String generatorType, Map<String, Object> values, FieldValue example,
    PluginV2.GenerateContentRequest.TestMode mode) {
    def builder = PluginV2.GenerateFieldRequest.newBuilder()
      .setKey(key)
      .setPath('$.one')
      .setExampleValue(example.toProto())
      .setTestMode(mode)
    if (generatorType != null) {
      builder.setGenerator(PluginV2.Generator.newBuilder()
        .setType(generatorType)
        .setValues(toStruct(values)))
    }
    builder.build()
  }

  private static Struct toStruct(Map<String, Object> values) {
    def struct = Struct.newBuilder()
    values.each { key, value ->
      def v = Value.newBuilder()
      switch (value) {
        case Number -> v.setNumberValue(((Number) value).doubleValue())
        case Boolean -> v.setBoolValue((Boolean) value)
        default -> v.setStringValue(value.toString())
      }
      struct.putFields(key, v.build())
    }
    struct.build()
  }

  def 'applies a core rule to a single value'() {
    given:
    def request = matchRequest('regex', 'regex', [regex: '\\d+'],
      new FieldValue.Text('100'), new FieldValue.Text('200'))

    when:
    def response = CoreFieldRuleMatcher.INSTANCE.matchField(request)

    then:
    response.error == ''
    response.mismatchesList.empty
  }

  def 'reports a mismatch against the path from the request'() {
    given:
    def request = matchRequest('regex', 'regex', [regex: '\\d+'],
      new FieldValue.Text('100'), new FieldValue.Text('not a number'))

    when:
    def response = CoreFieldRuleMatcher.INSTANCE.matchField(request)

    then:
    response.error == ''
    response.mismatchesCount == 1
    response.getMismatches(0).path == '$.one'
    response.getMismatches(0).mismatchType == 'body'
    response.getMismatches(0).actual.value.toStringUtf8() == 'not a number'
    response.getMismatches(0).mismatch.contains('to match')
  }

  /**
   * The distinction FieldValue's per-type arms exist for: an integer actual against a decimal
   * expected is a type mismatch, and would not be if both arrived as one JSON number type.
   */
  def 'keeps the numeric type of a value'() {
    when:
    def integer = CoreFieldRuleMatcher.INSTANCE.matchField(matchRequest('integer', 'integer', [:],
      new FieldValue.Integer(100L), new FieldValue.Integer(200L)))
    def decimal = CoreFieldRuleMatcher.INSTANCE.matchField(matchRequest('decimal', 'decimal', [:],
      new FieldValue.Decimal(100.0), new FieldValue.Integer(200L)))

    then:
    integer.mismatchesList.empty
    decimal.mismatchesCount == 1
  }

  def 'falls back to the catalogue key when the request carries no rule'() {
    given:
    def request = matchRequest('not-empty', null, [:], new FieldValue.Text('a'), new FieldValue.Text(''))

    when:
    def response = CoreFieldRuleMatcher.INSTANCE.matchField(request)

    then:
    response.error == ''
    response.mismatchesCount == 1
  }

  def 'compares a binary value as bytes'() {
    given:
    def expected = new FieldValue.Binary([0x00, 0xfe, 0x01] as byte[])
    def actual = new FieldValue.Binary([0x00, 0xfe, 0x02] as byte[])
    def request = matchRequest('equality', 'equality', [:], expected, actual)

    when:
    def response = CoreFieldRuleMatcher.INSTANCE.matchField(request)

    then:
    response.mismatchesCount == 1
    response.getMismatches(0).actual.value.toByteArray() == [0x00, 0xfe, 0x02] as byte[]
  }

  def 'a rule this framework does not provide is an error, not a call back out to a plugin'() {
    given:
    def request = matchRequest('type', 'creditcard', [brand: 'visa'],
      new FieldValue.Text('4111111111111111'), new FieldValue.Text('4111111111111111'))

    when:
    def response = CoreFieldRuleMatcher.INSTANCE.matchField(request)

    then:
    response.error == "'creditcard' is not one of the matching rules provided by this framework"
    response.mismatchesList.empty
  }

  def 'a collection-wide rule says why it can not be applied'() {
    given:
    def request = matchRequest('each-value', null, [:],
      new FieldValue.Text('a'), new FieldValue.Text('b'))

    when:
    def response = CollectionRuleMatcher.INSTANCE.matchField(request)

    then:
    response.error.contains('applies to a collection as a whole')
    response.mismatchesList.empty
  }

  def 'generates a single value'() {
    given:
    def request = generateRequest('RandomInt', 'RandomInt', [min: 5, max: 5],
      new FieldValue.Integer(0L), PluginV2.GenerateContentRequest.TestMode.Consumer)

    when:
    def response = CoreFieldValueGenerator.INSTANCE.generateField(request)

    then:
    response.error == ''
    FieldValue.fromProto(response.value) == new FieldValue.Integer(5L)
  }

  def 'a generator for the other side of the test leaves the example value alone'() {
    given:
    // MockServerURL only applies on the consumer side
    def request = generateRequest('MockServerURL', 'MockServerURL',
      [example: 'http://localhost:1234/a', regex: '.*(/a)'],
      new FieldValue.Text('http://localhost:1234/a'), PluginV2.GenerateContentRequest.TestMode.Provider)

    when:
    def response = CoreFieldValueGenerator.INSTANCE.generateField(request)

    then:
    response.error == ''
    FieldValue.fromProto(response.value) == new FieldValue.Text('http://localhost:1234/a')
  }

  def 'a generator this framework does not provide is an error'() {
    given:
    def request = generateRequest('Uuid', 'creditcard', [brand: 'visa'],
      new FieldValue.Text('4111111111111111'), PluginV2.GenerateContentRequest.TestMode.Consumer)

    when:
    def response = CoreFieldValueGenerator.INSTANCE.generateField(request)

    then:
    response.error == "'creditcard' is not one of the generators provided by this framework"
    !response.hasValue()
  }

  def 'a collection generator says why it can not be applied'() {
    given:
    def request = generateRequest('ArrayContains', null, [:], FieldValue.Null.INSTANCE,
      PluginV2.GenerateContentRequest.TestMode.Consumer)

    when:
    def response = CollectionValueGenerator.INSTANCE.generateField(request)

    then:
    response.error.contains('generates values within a collection')
  }

  /**
   * End to end through the driver: the catalogue entry registerCoreCapabilities registers, the
   * resolver a plugin's callback goes through, and the handler registered under that key. A key
   * registered on one side but not the other would only show up here.
   */
  def 'a plugin callback for a core rule reaches the registered handler'() {
    given:
    MatchingConfig.INSTANCE.registerCoreCapabilities()
    def matcher = FieldKt.findFieldMatcher('type')
    def context = new FieldContext('$.one', 'body', null, [:])

    when:
    def matched = matcher.matchField(TypeMatcher.INSTANCE, new FieldValue.Text('a'),
      new FieldValue.Text('b'), context)
    def mismatched = matcher.matchField(TypeMatcher.INSTANCE, new FieldValue.Text('a'),
      new FieldValue.Integer(100L), context)

    then:
    matcher.core
    matched.empty
    mismatched.size() == 1
    mismatched[0].path == '$.one'
  }

  /** The generator half of the callback round trip. */
  def 'a plugin callback for a core generator reaches the registered handler'() {
    given:
    MatchingConfig.INSTANCE.registerCoreCapabilities()
    def generator = FieldKt.findFieldGenerator('Uuid')
    def context = new FieldContext('$.one', 'body', null, [:])

    when:
    def generated = generator.generateField(new UuidGenerator(), new FieldValue.Text(''),
      FieldTestMode.CONSUMER, context)

    then:
    generator.core
    generated instanceof FieldValue.Text
    ((FieldValue.Text) generated).value.length() == 36
  }

  /**
   * Every key the catalogue advertises has a handler behind it, and no handler is registered for a
   * key that is not advertised - the two lists are what proposal 009 step 3 is.
   */
  def 'every registered field handler matches a catalogue entry'() {
    given:
    def rules = (CoreFieldCapabilitiesKt.FIELD_RULES + CoreFieldCapabilitiesKt.COLLECTION_RULES) as Set
    def generators = (CoreFieldCapabilitiesKt.FIELD_GENERATORS + CoreFieldCapabilitiesKt.COLLECTION_GENERATORS) as Set
    def ruleEntries = MatcherExecutorKt.matcherCatalogueEntries()*.key as Set
    def generatorEntries = MatcherExecutorKt.generatorCatalogueEntries()*.key as Set

    expect:
    rules == ruleEntries
    generators == generatorEntries
  }
}
