package au.com.dius.pact.core.matchers

import com.google.protobuf.ByteString
import com.google.protobuf.BytesValue
import com.google.protobuf.Struct
import com.google.protobuf.Value
import io.pact.plugin.Plugin
import io.pact.plugins.jvm.core.CoreCapabilityRegistry
import spock.lang.Specification

// resultsMap is a protobuf-generated Map view; Groovy's isEmpty()-as-property shorthand doesn't resolve
// against it, so the explicit method call is required here, not just a style preference.
@SuppressWarnings('UnnecessaryGetter')
class CoreCapabilitiesSpec extends Specification {

  def 'registers the core content matcher and generator handlers'() {
    when:
    MatchingConfig.INSTANCE.registerCoreCapabilities()

    then:
    ['xml', 'json', 'text', 'multipart-form-data', 'form-urlencoded'].every {
      CoreCapabilityRegistry.INSTANCE.contentMatcher(it) != null
    }
    CoreCapabilityRegistry.INSTANCE.contentGenerator('json') != null
  }

  private static Plugin.Body body(String content, String contentType = 'application/json') {
    Plugin.Body.newBuilder()
      .setContent(BytesValue.newBuilder().setValue(ByteString.copyFromUtf8(content)))
      .setContentType(contentType)
      .build()
  }

  def 'JSON core content matcher - matching bodies produce no mismatches'() {
    given:
    def handler = new CoreContentMatcherAdapter(JsonContentMatcher.INSTANCE)
    def request = Plugin.CompareContentsRequest.newBuilder()
      .setExpected(body('{"name":"harry"}'))
      .setActual(body('{"name":"harry"}'))
      .build()

    when:
    def response = handler.compareContents(request)

    then:
    response.resultsMap.isEmpty()
    !response.hasTypeMismatch()
  }

  def 'JSON core content matcher - mismatched bodies are reported by path'() {
    given:
    def handler = new CoreContentMatcherAdapter(JsonContentMatcher.INSTANCE)
    def request = Plugin.CompareContentsRequest.newBuilder()
      .setExpected(body('{"name":"harry"}'))
      .setActual(body('{"name":"fred"}'))
      .build()

    when:
    def response = handler.compareContents(request)

    then:
    response.resultsMap.keySet() == ['$.name'] as Set
    response.resultsMap['$.name'].mismatchesList.size() == 1
    response.resultsMap['$.name'].mismatchesList[0].mismatchType == 'body'
  }

  def 'JSON core content matcher - honours matching rules from the request'() {
    given:
    def handler = new CoreContentMatcherAdapter(JsonContentMatcher.INSTANCE)
    def rule = Plugin.MatchingRule.newBuilder().setType('type').build()
    def request = Plugin.CompareContentsRequest.newBuilder()
      .setExpected(body('{"name":"harry"}'))
      .setActual(body('{"name":"fred"}'))
      .putRules('$.name', Plugin.MatchingRules.newBuilder().addRule(rule).build())
      .build()

    when:
    def response = handler.compareContents(request)

    then:
    response.resultsMap.isEmpty()
  }

  def 'JSON core content generator - applies the requested generator'() {
    given:
    def generator = Plugin.Generator.newBuilder()
      .setType('RandomInt')
      .setValues(Struct.newBuilder()
        .putFields('min', Value.newBuilder().setNumberValue(1).build())
        .putFields('max', Value.newBuilder().setNumberValue(1).build())
        .build())
      .build()
    def request = Plugin.GenerateContentRequest.newBuilder()
      .setContents(body('{"id":"1"}'))
      .putGenerators('$.id', generator)
      .setTestMode(Plugin.GenerateContentRequest.TestMode.Consumer)
      .build()

    when:
    def response = JsonCoreContentGenerator.INSTANCE.generateContent(request)

    then:
    new String(response.contents.content.value.toByteArray(), 'UTF-8') == '{"id":1}'
  }
}
