package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.ContentTypeHint
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.createGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.support.json.JsonValue
import com.google.protobuf.ByteString
import com.google.protobuf.BytesValue
import io.pact.plugin.Plugin
import io.pact.plugins.jvm.core.CoreContentGenerator
import io.pact.plugins.jvm.core.CoreContentMatcher
import io.pact.plugins.jvm.core.Utils.structToJson
import io.pact.plugins.jvm.core.Utils.structToMap

/**
 * Adapts a native [ContentMatcher] so it can be registered as a host-provided ("core") content matcher
 * capability (pact-plugins proposal 009), letting a plugin delegate whole-content-type matching back to
 * this framework instead of reproducing the logic itself. Registered against [CoreCapabilityRegistry] by
 * [MatchingConfig.registerCoreCapabilities].
 *
 * There is no field-level equivalent yet: that needs proposal 006's field-level operation shape, which
 * the plugin driver does not implement yet.
 */
class CoreContentMatcherAdapter(private val matcher: ContentMatcher) : CoreContentMatcher {
  override fun compareContents(request: Plugin.CompareContentsRequest): Plugin.CompareContentsResponse {
    val expected = toOptionalBody(request.expected)
    val actual = toOptionalBody(request.actual)
    val rules = MatchingRuleCategory("body", request.rulesMap.mapValues { (_, ruleSet) ->
      MatchingRuleGroup(ruleSet.ruleList.map { MatchingRule.create(it.type, structToJson(it.values)) }.toMutableList())
    }.toMutableMap())

    // The plugin-configuration map is keyed by plugin name so a core matcher can recurse into another
    // plugin's own matcher for embedded content; the request only carries a single (unnamed) config, so
    // there is no name to key it under here. Nested plugin delegation from within core matching is out of
    // scope until proposal 006's field-level shape exists.
    val context = MatchingContext(rules, request.allowUnexpectedKeys)

    val result = matcher.matchBody(expected, actual, context)
    val builder = Plugin.CompareContentsResponse.newBuilder()
    val typeMismatch = result.typeMismatch
    if (typeMismatch != null) {
      builder.setTypeMismatch(Plugin.ContentTypeMismatch.newBuilder()
        .setExpected(typeMismatch.expected.orEmpty())
        .setActual(typeMismatch.actual.orEmpty())
        .build())
    } else {
      result.bodyResults
        .filter { it.result.isNotEmpty() }
        .groupBy({ it.key }, { it.result })
        .forEach { (path, mismatchLists) ->
          builder.putResults(path, Plugin.ContentMismatches.newBuilder()
            .addAllMismatches(mismatchLists.flatten().map(::toContentMismatch))
            .build())
        }
    }
    return builder.build()
  }
}

/**
 * Adapts [Generators.applyBodyGenerators] so it can be registered as the host-provided ("core") JSON
 * content generator capability. Registered against [CoreCapabilityRegistry] by
 * [MatchingConfig.registerCoreCapabilities].
 */
object JsonCoreContentGenerator : CoreContentGenerator {
  override fun generateContent(request: Plugin.GenerateContentRequest): Plugin.GenerateContentResponse {
    val contentType = ContentType(request.contents.contentType)
    val body = OptionalBody.body(request.contents.content.value.toByteArray(), contentType)
    val generators = request.generatorsMap.mapValues { (_, generator) ->
      createGenerator(generator.type, structToJson(generator.values))
    }
    val testMode = if (request.testMode == Plugin.GenerateContentRequest.TestMode.Consumer)
      GeneratorTestMode.Consumer else GeneratorTestMode.Provider
    val context = structToMap(request.testContext)
      .filterValues { it != null }
      .mapValues { it.value!! }
      .toMutableMap()

    val generated = Generators.applyBodyGenerators(generators, body, contentType, context, testMode)

    return Plugin.GenerateContentResponse.newBuilder()
      .setContents(Plugin.Body.newBuilder()
        .setContent(BytesValue.newBuilder().setValue(ByteString.copyFrom(generated.orEmpty())))
        .setContentType(contentType.toString()))
      .build()
  }
}

private fun toOptionalBody(body: Plugin.Body): OptionalBody {
  return OptionalBody.body(body.content.value.toByteArray(), ContentType(body.contentType),
    toContentTypeHint(body.contentTypeHint))
}

private fun toContentTypeHint(hint: Plugin.Body.ContentTypeHint): ContentTypeHint {
  return when (hint) {
    Plugin.Body.ContentTypeHint.TEXT -> ContentTypeHint.TEXT
    Plugin.Body.ContentTypeHint.BINARY -> ContentTypeHint.BINARY
    else -> ContentTypeHint.DEFAULT
  }
}

private fun toBytesValue(value: Any?): BytesValue {
  val text = when (value) {
    null -> ""
    is JsonValue -> value.serialise()
    else -> value.toString()
  }
  return BytesValue.newBuilder().setValue(ByteString.copyFromUtf8(text)).build()
}

private fun toContentMismatch(mismatch: BodyMismatch) = Plugin.ContentMismatch.newBuilder()
  .setExpected(toBytesValue(mismatch.expected))
  .setActual(toBytesValue(mismatch.actual))
  .setMismatch(mismatch.mismatch)
  .setPath(mismatch.path)
  .setDiff(mismatch.diff.orEmpty())
  .setMismatchType(mismatch.type())
  .build()
