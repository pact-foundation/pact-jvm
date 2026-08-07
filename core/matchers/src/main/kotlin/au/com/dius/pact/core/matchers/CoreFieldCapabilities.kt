package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.generators.PluginGenerator
import au.com.dius.pact.core.model.generators.createGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.PluginMatcher
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import com.google.protobuf.ByteString
import com.google.protobuf.BytesValue
import com.google.protobuf.Struct
import io.github.oshai.kotlinlogging.KotlinLogging
import io.pact.plugin.v2.PluginV2
import io.pact.plugins.jvm.core.CoreFieldGenerator
import io.pact.plugins.jvm.core.CoreFieldMatcher
import io.pact.plugins.jvm.core.FieldValue
import io.pact.plugins.jvm.core.Utils.structToJson

private val logger = KotlinLogging.logger {}

/**
 * The matching rules this framework can apply to a single value, and so registers a field-level
 * handler for. Every other rule it implements is collection-wide (see [COLLECTION_RULES]).
 */
internal val FIELD_RULES = listOf("equality", "regex", "type", "include", "number", "integer",
  "decimal", "boolean", "null", "date", "time", "datetime", "content-type", "not-empty", "semver",
  "status-code")

/**
 * The matching rules that decide *which* values they apply to, and so can not be handed one value
 * at a time - see proposal 006's non-goals. They get a handler anyway, so a plugin naming one is
 * told why it can not have it rather than being told nothing is registered.
 */
internal val COLLECTION_RULES = listOf("min-type", "max-type", "min-max-type", "values",
  "array-contains", "each-key", "each-value", "ignore-order", "min-ignore-order", "max-ignore-order",
  "min-max-ignore-order")

/**
 * The generators this framework can apply to a single value. `ProviderState` and `MockServerURL`
 * are included: both read host state, but it reaches them through the request's test context, which
 * is what proposal 006 requires of any generator.
 */
internal val FIELD_GENERATORS = listOf("RandomInt", "RandomDecimal", "RandomHexadecimal",
  "RandomString", "RandomBoolean", "Regex", "Uuid", "Date", "Time", "DateTime", "ProviderState",
  "MockServerURL", "Null")

/**
 * Generators that build a value inside a structure their caller owns, so there is no single value
 * to generate. See [COLLECTION_RULES].
 */
internal val COLLECTION_GENERATORS = listOf("ArrayContains")

/**
 * Applies one of this framework's standard matching rules to a single value, on behalf of a plugin
 * that does not want to reimplement it (proposal 009). Registered against every key in
 * [FIELD_RULES] - the rule to apply comes from the request, so one handler serves them all.
 */
object CoreFieldRuleMatcher : CoreFieldMatcher {
  // A handler answers a plugin over gRPC: anything that goes wrong has to come back as the
  // response's error field, so nothing may propagate out of here
  @Suppress("TooGenericExceptionCaught")
  override fun matchField(request: PluginV2.MatchFieldRequest): PluginV2.MatchFieldResponse {
    val rule = try {
      ruleFromRequest(request)
    } catch (ex: Exception) {
      logger.debug(ex) { "Could not build the matching rule for a MatchField request" }
      return PluginV2.MatchFieldResponse.newBuilder().setError(ex.message.orEmpty()).build()
    }

    val expected = FieldValue.fromProto(request.expected)
    val actual = FieldValue.fromProto(request.actual)
    logger.debug { "Applying the core '${rule.name}' matching rule to the value at ${request.path}" }

    val mismatches = domatch(rule, listOf(request.path), forMatching(expected), forMatching(actual),
      BodyMismatchFactory, false, null)

    return PluginV2.MatchFieldResponse.newBuilder()
      .addAllMismatches(mismatches.map { toFieldMismatch(request, expected, actual, it.mismatch) })
      .build()
  }
}

/**
 * Answers for the collection-wide rules: they are real capabilities this framework has, so they are
 * in the catalogue, but the matching engine has to interpret them itself to know which values they
 * cover. See [COLLECTION_RULES].
 */
object CollectionRuleMatcher : CoreFieldMatcher {
  override fun matchField(request: PluginV2.MatchFieldRequest): PluginV2.MatchFieldResponse {
    return PluginV2.MatchFieldResponse.newBuilder()
      .setError("The '${request.key}' matching rule applies to a collection as a whole, not to a " +
        "single value, so it can not be applied through MatchField. Match the collection with a " +
        "content matcher instead.")
      .build()
  }
}

/**
 * Applies one of this framework's standard generators to a single value, on behalf of a plugin
 * (proposal 009). Registered against every key in [FIELD_GENERATORS].
 */
object CoreFieldValueGenerator : CoreFieldGenerator {
  // See CoreFieldRuleMatcher.matchField
  @Suppress("TooGenericExceptionCaught", "ReturnCount")
  override fun generateField(request: PluginV2.GenerateFieldRequest): PluginV2.GenerateFieldResponse {
    val generator = try {
      generatorFromRequest(request)
    } catch (ex: Exception) {
      logger.debug(ex) { "Could not build the generator for a GenerateField request" }
      return PluginV2.GenerateFieldResponse.newBuilder().setError(ex.message.orEmpty()).build()
    }

    val example = FieldValue.fromProto(request.exampleValue)
    // A generator that does not apply on this side of the test leaves the example value alone, the
    // same as it would when applied to a body. An unknown mode applies it rather than guessing a
    // side: guessing wrong turns `MockServerURL` into a silent no-op in a consumer test.
    val mode = when (request.testMode) {
      PluginV2.GenerateContentRequest.TestMode.Consumer -> GeneratorTestMode.Consumer
      PluginV2.GenerateContentRequest.TestMode.Provider -> GeneratorTestMode.Provider
      else -> null
    }
    if (mode != null && !generator.correspondsToMode(mode)) {
      return PluginV2.GenerateFieldResponse.newBuilder().setValue(example.toProto()).build()
    }

    val context = structToMapOfAny(request.testContext)
    logger.debug { "Applying the core '${generator.type}' generator to the value at ${request.path}" }

    return try {
      val generated = generator.generate(context, forMatching(example))
      PluginV2.GenerateFieldResponse.newBuilder()
        .setValue(toFieldValue(generated).toProto())
        .build()
    } catch (ex: Exception) {
      logger.debug(ex) { "The '${generator.type}' generator failed" }
      PluginV2.GenerateFieldResponse.newBuilder()
        .setError("The '${generator.type}' generator could not generate a value - ${ex.message}")
        .build()
    }
  }
}

/**
 * Answers for generators that build values inside a structure their caller owns. See
 * [CollectionRuleMatcher].
 */
object CollectionValueGenerator : CoreFieldGenerator {
  override fun generateField(request: PluginV2.GenerateFieldRequest): PluginV2.GenerateFieldResponse {
    return PluginV2.GenerateFieldResponse.newBuilder()
      .setError("The '${request.key}' generator generates values within a collection, not a single " +
        "value, so it can not be applied through GenerateField.")
      .build()
  }
}

/**
 * The rule named by a field-level request: the rule it carries, or - if it carries none - the
 * catalogue key it was dispatched under, so `host_match_field("not-empty", ...)` works without
 * having to restate the rule.
 */
private fun ruleFromRequest(request: PluginV2.MatchFieldRequest): MatchingRule {
  val name = request.rule.type.ifEmpty { request.key }
  // A core handler answers for core rules only. Checking the catalogue first keeps the answer the
  // same whatever an unrecognised name does further in - MatchingRule.create turns it into a
  // PluginMatcher, which would send the call straight back out to a plugin.
  require(matcherCatalogueEntries().any { it.key == name }) {
    "'$name' is not one of the matching rules provided by this framework"
  }
  val values = wholeNumbersToIntegers(structToJson(request.rule.values))
  val rule = MatchingRule.create(name, values)
  require(rule !is PluginMatcher) {
    "'$name' is not one of the matching rules provided by this framework"
  }
  return rule
}

/** The generator named by a field-level request. See [ruleFromRequest]. */
private fun generatorFromRequest(request: PluginV2.GenerateFieldRequest): Generator {
  val name = request.generator.type.ifEmpty { request.key }
  // See ruleFromRequest - and createGenerator resolves a generator class by name reflectively, so
  // an unrecognised name would otherwise surface as a ClassNotFoundException
  require(generatorCatalogueEntries().any { it.key == name }) {
    "'$name' is not one of the generators provided by this framework"
  }
  val values = wholeNumbersToIntegers(structToJson(request.generator.values))
  val generator = createGenerator(name, values)
  require(generator !is PluginGenerator) {
    "'$name' is not one of the generators provided by this framework"
  }
  return generator
}

/**
 * Rule and generator configuration crosses the plugin interface as a `google.protobuf.Struct`,
 * which has one number type - a double - so a `min` of 2 arrives as `2.0`. Whole numbers are put
 * back to integers here so a configuration value means what the plugin sent, whatever the receiving
 * `fromJson` does with a decimal.
 *
 * This is the configuration-value counterpart of what [FieldValue]'s per-type arms do for the value
 * being matched, which does not go through a `Struct` for exactly this reason.
 */
private fun wholeNumbersToIntegers(value: JsonValue): JsonValue = when (value) {
  is JsonValue.Decimal -> {
    val number = value.toBigDecimal()
    if (number.stripTrailingZeros().scale() <= 0) {
      JsonValue.Integer(number.toBigInteger().toString().toCharArray())
    } else {
      value
    }
  }
  is JsonValue.Array -> JsonValue.Array(value.values.map { wholeNumbersToIntegers(it) }.toMutableList())
  is JsonValue.Object -> JsonValue.Object(value.entries
    .map { it.key to wholeNumbersToIntegers(it.value) }
    .toMap()
    .toMutableMap())
  else -> value
}

/**
 * The form the matching and generation code works with. Unlike [fromFieldValue], which converts a
 * generated value back into something a document can hold, this keeps a binary value as bytes -
 * `content-type` and `equality` both have something meaningful to say about raw bytes.
 */
private fun forMatching(value: FieldValue): Any? = when (value) {
  is FieldValue.Null -> null
  is FieldValue.Bool -> value.value
  is FieldValue.Text -> value.value
  is FieldValue.Integer -> value.value
  is FieldValue.Decimal -> value.value
  is FieldValue.Binary -> value.value
  is FieldValue.Structured -> value.value
}

private fun toFieldMismatch(
  request: PluginV2.MatchFieldRequest,
  expected: FieldValue,
  actual: FieldValue,
  mismatch: String
): PluginV2.ContentMismatch = PluginV2.ContentMismatch.newBuilder()
  .setExpected(fieldValueBytes(expected))
  .setActual(fieldValueBytes(actual))
  .setMismatch(mismatch)
  .setPath(request.path)
  .setMismatchType(request.mismatchType)
  .build()

/**
 * The bytes a mismatch reports for a value. `ContentMismatch` carries bytes so that a binary value
 * survives being reported, which means every other value has to be rendered into some form here.
 */
private fun fieldValueBytes(value: FieldValue): BytesValue {
  val bytes = when (value) {
    is FieldValue.Null -> ByteArray(0)
    is FieldValue.Binary -> value.value
    is FieldValue.Text -> value.value.toByteArray()
    is FieldValue.Structured -> value.value.serialise().toByteArray()
    is FieldValue.Bool -> value.value.toString().toByteArray()
    is FieldValue.Integer -> value.value.toString().toByteArray()
    is FieldValue.Decimal -> value.value.toString().toByteArray()
  }
  return BytesValue.newBuilder().setValue(ByteString.copyFrom(bytes)).build()
}

/** The test context a generator reads, in the form [Generator.generate] takes it. */
private fun structToMapOfAny(struct: Struct): MutableMap<String, Any> {
  val json = structToJson(struct)
  return if (json is JsonValue.Object) {
    json.entries
      .mapNotNull { (key, value) -> Json.fromJson(value)?.let { key to it } }
      .toMap()
      .toMutableMap()
  } else {
    mutableMapOf()
  }
}
