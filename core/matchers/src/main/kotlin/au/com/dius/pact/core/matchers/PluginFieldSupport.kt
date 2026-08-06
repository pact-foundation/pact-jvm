package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.generators.PluginGenerator
import au.com.dius.pact.core.model.matchingrules.PluginMatcher
import au.com.dius.pact.core.model.plugins.PluginSupport
import au.com.dius.pact.core.model.constructPath
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import io.github.oshai.kotlinlogging.KotlinLogging
import io.pact.plugins.jvm.core.FieldContext
import io.pact.plugins.jvm.core.FieldTestMode
import io.pact.plugins.jvm.core.FieldValue
import io.pact.plugins.jvm.core.PluginConfiguration
import io.pact.plugins.jvm.core.TestContext
import io.pact.plugins.jvm.core.findFieldGenerator
import io.pact.plugins.jvm.core.findFieldMatcher
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Applies a plugin-provided matching rule to a single value, resolving the rule name against the
 * plugin catalogue.
 *
 * This is the field-level counterpart of [PluginContentMatcher]: where that hands a whole body to
 * a plugin that owns the content type, this hands one value to a plugin that owns one rule. See
 * proposal 006, Field-level matchers and generators.
 *
 * @param path Path to the value, as a Pact matching rule expression
 * @param category Part of the interaction the value came from: `body`, `header`, `metadata`, ...
 */
fun <M : Mismatch> matchPluginRule(
  matcher: PluginMatcher,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>,
  category: String,
  pluginConfiguration: PluginConfiguration?
): List<M> {
  if (TestContext.currentTestRunId() == null) {
    TestContext.setTestRunId(UUID.randomUUID().toString())
  }

  val fieldMatcher = try {
    findFieldMatcher(matcher.ruleName)
  } catch (ex: Exception) {
    logger.error(ex) { "Could not resolve the '${matcher.ruleName}' matching rule" }
    return listOf(mismatchFactory.create(expected, actual,
      "Could not apply the '${matcher.ruleName}' matching rule - ${ex.message}", path))
  }

  val context = FieldContext(
    path = constructPath(path),
    category = category,
    pluginConfiguration = pluginConfiguration
  )
  logger.debug { "Applying the '${matcher.ruleName}' matching rule provided by ${fieldMatcher.pluginName}" }

  return fieldMatcher.matchField(matcher, toFieldValue(expected), toFieldValue(actual), context)
    .map { mismatchFactory.create(expected, actual, it.mismatch, path) }
}

/**
 * Resolves plugin-provided rules and generators on behalf of `core:model`, which has no visibility
 * of the plugin catalogue. See [PluginSupport].
 */
object DriverPluginSupport : PluginSupport {
  override fun configKey(ruleName: String): String? = try {
    findFieldMatcher(ruleName).catalogueEntry.values["config-key"]
  } catch (ex: Exception) {
    logger.debug(ex) { "No catalogue entry for the '$ruleName' matching rule, so no config-key" }
    null
  }

  override fun generate(
    name: String,
    values: Map<String, JsonValue>,
    exampleValue: Any?,
    path: String,
    context: Map<String, Any>
  ): Any? {
    if (TestContext.currentTestRunId() == null) {
      TestContext.setTestRunId(UUID.randomUUID().toString())
    }

    val generator = findFieldGenerator(name)
    // The category only affects how a mismatch is reported, which generation has no equivalent of
    val fieldContext = FieldContext(path = path, category = "body")
    logger.debug { "Applying the '$name' generator provided by ${generator.pluginName}" }

    val generated = generator.generateField(
      PluginGenerator(name, values),
      toFieldValue(exampleValue),
      FieldTestMode.UNKNOWN,
      fieldContext
    )
    return fromFieldValue(generated)
  }
}

/**
 * Converts a value from the matching/generation path into the form the plugin interface carries.
 *
 * The distinction between a whole number and a decimal has to survive this: `integer`, `decimal`
 * and `type` are exactly the rules that would break if it did not, which is why `FieldValue` has
 * an arm per type rather than putting everything through one JSON-ish value.
 */
internal fun toFieldValue(value: Any?): FieldValue = when (value) {
  null -> FieldValue.Null
  is FieldValue -> value
  is ByteArray -> FieldValue.Binary(value)
  is JsonValue -> FieldValue.fromJson(value)
  is Boolean -> FieldValue.Bool(value)
  is Int -> FieldValue.Integer(value.toLong())
  is Long -> FieldValue.Integer(value)
  is Short -> FieldValue.Integer(value.toLong())
  is Byte -> FieldValue.Integer(value.toLong())
  is java.math.BigInteger -> FieldValue.Integer(value.toLong())
  is Float -> FieldValue.Decimal(value.toDouble())
  is Double -> FieldValue.Decimal(value)
  is java.math.BigDecimal -> FieldValue.Decimal(value.toDouble())
  is String -> FieldValue.Text(value)
  else -> FieldValue.fromJson(Json.toJson(value))
}

/** Converts a value a plugin generated back into the form the document can hold */
internal fun fromFieldValue(value: FieldValue): Any? = when (value) {
  is FieldValue.Null -> null
  is FieldValue.Bool -> value.value
  is FieldValue.Text -> value.value
  is FieldValue.Integer -> value.value
  is FieldValue.Decimal -> value.value
  is FieldValue.Structured -> Json.fromJson(value.value)
  // A generator applied to a value in a document has to produce something the document can hold,
  // so binary is only usable here if it is text
  is FieldValue.Binary -> String(value.value)
}
