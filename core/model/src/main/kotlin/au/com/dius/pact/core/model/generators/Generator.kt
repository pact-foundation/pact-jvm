package au.com.dius.pact.core.model.generators

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOr
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser.containsExpressions
import au.com.dius.pact.core.support.expressions.ExpressionParser.parseExpression
import au.com.dius.pact.core.support.expressions.MapValueResolver
import au.com.dius.pact.core.support.json.JsonValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.mifmif.common.regex.Generex
import mu.KLogging
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.regex.PatternSyntaxException
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberFunctions

private val logger = KotlinLogging.logger {}

const val DEFAULT_GENERATOR_PACKAGE = "au.com.dius.pact.core.model.generators"

/**
 * Looks up the generator class in the configured generator packages. By default it will look for generators in
 * au.com.dius.pact.model.generators package, but this can be extended by adding a comma separated list to the
 * pact.generators.packages system property. The generator class name needs to be <Type>Generator.
 */
fun lookupGenerator(generatorJson: JsonValue?): Generator? {
  var generator: Generator? = null

  if (generatorJson is JsonValue.Object) {
    try {
      generator = createGenerator(Json.toString(generatorJson["type"]), generatorJson)
    } catch (e: ClassNotFoundException) {
      logger.warn(e) { "Could not find generator class for generator config '$generatorJson'" }
    } catch (e: InvalidGeneratorException) {
      logger.warn(e) { e.message }
    }
  } else {
    logger.warn { "'$generatorJson' is not a valid generator JSON value" }
  }

  return generator
}

fun createGenerator(type: String, generatorJson: JsonValue): Generator {
  val generatorClass = findGeneratorClass(type).kotlin
  val (instance, fromJson) = when {
    generatorClass.companionObject != null ->
      generatorClass.companionObjectInstance to generatorClass.companionObject?.declaredMemberFunctions?.find {
        it.name == "fromJson"
      }
    generatorClass.objectInstance != null ->
      generatorClass.objectInstance to generatorClass.declaredMemberFunctions.find { it.name == "fromJson" }
    else -> null to null
  }
  if (fromJson != null) {
    return fromJson.call(instance, generatorJson) as Generator
  } else {
    throw InvalidGeneratorException("Could not invoke generator class 'fromJson' for generator config '$generatorJson'")
  }
}

class InvalidGeneratorException(message: String) : RuntimeException(message)

fun findGeneratorClass(generatorType: String): Class<*> {
  val generatorPackages = System.getProperty("pact.generators.packages")
  return when {
    generatorPackages.isNullOrBlank() -> Class.forName("$DEFAULT_GENERATOR_PACKAGE.${generatorType}Generator")
    else -> {
      val packages = generatorPackages.split(",").map { it.trim() } + DEFAULT_GENERATOR_PACKAGE
      var generatorClass: Class<*>? = null

      packages.find {
        try {
          generatorClass = Class.forName("$it.${generatorType}Generator")
          true
        } catch (_: ClassNotFoundException) {
          false
        }
      }

      generatorClass ?: throw ClassNotFoundException("No generator found for type '$generatorType'")
    }
  }
}

/**
 * Interface that all Generators need to implement
 */
interface Generator {
  val type: String
  fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any?
  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any>
  fun correspondsToMode(mode: GeneratorTestMode): Boolean = true
}

/**
 * Generates a random integer between a min and max value
 */
data class RandomIntGenerator(val min: Int, val max: Int) : Generator {
  override val type: String
    get() = "RandomInt"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to type, "min" to min, "max" to max)
  }

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any {
    return RandomUtils.nextInt(min, max)
  }

  companion object {
    fun fromJson(json: JsonValue.Object): RandomIntGenerator {
      val min = if (json["min"].isNumber) {
        json["min"].asNumber()!!.toInt()
      } else {
        logger.warn { "Ignoring invalid value for min: '${json["min"]}'" }
        0
      }
      val max = if (json["max"].isNumber) {
        json["max"].asNumber()!!.toInt()
      } else {
        logger.warn { "Ignoring invalid value for max: '${json["max"]}'" }
        Int.MAX_VALUE
      }
      return RandomIntGenerator(min, max)
    }
  }
}

/**
 * Generates a random big decimal value with the provided number of digits
 */
data class RandomDecimalGenerator(val digits: Int) : Generator {
  override val type: String
    get() = "RandomDecimal"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to type, "digits" to digits)
  }

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any {
    return when {
      digits < 1 -> throw UnsupportedOperationException("RandomDecimalGenerator digits must be > 0, got $digits")
      digits == 1 -> BigDecimal(RandomUtils.nextInt(0, 9))
      digits == 2 -> BigDecimal("${RandomUtils.nextInt(0, 9)}.${RandomUtils.nextInt(0, 9)}")
      else -> {
        val sampleDigits = RandomStringUtils.randomNumeric(digits + 1)
        val pos = RandomUtils.nextInt(1, digits - 1)
        val selectedDigits = if (sampleDigits.startsWith("00")) {
          RandomUtils.nextInt(1, 9).toString() + sampleDigits.substring(1, digits)
        } else if (pos != 1 && sampleDigits.startsWith('0')) {
          sampleDigits.substring(1)
        } else {
          sampleDigits.substring(0, digits)
        }
        val generated = "${selectedDigits.substring(0, pos)}.${selectedDigits.substring(pos)}"
        logger.trace {
          "RandomDecimalGenerator: sampleDigits=[$sampleDigits], pos=$pos, selectedDigits=[$selectedDigits], " +
            "generated=[$generated]"
        }
        BigDecimal(generated)
      }
    }
  }

  companion object {
    fun fromJson(json: JsonValue.Object): RandomDecimalGenerator {
      val digits = if (json["digits"].isNumber) {
        json["digits"].asNumber()!!.toInt()
      } else {
        logger.warn { "Ignoring invalid value for digits: '${json["digits"]}'" }
        10
      }
      return RandomDecimalGenerator(digits)
    }
  }
}

/**
 * Generates a random hexadecimal value of the given number of digits
 */
data class RandomHexadecimalGenerator(val digits: Int) : Generator {
  override val type: String
    get() = "RandomHexadecimal"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to type, "digits" to digits)
  }

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any =
    RandomStringUtils.random(digits, "0123456789abcdef")

  companion object {
    fun fromJson(json: JsonValue.Object): RandomHexadecimalGenerator {
      val digits = if (json["digits"].isNumber) {
        json["digits"].asNumber()!!.toInt()
      } else {
        logger.warn { "Ignoring invalid value for digits: '${json["digits"]}'" }
        10
      }
      return RandomHexadecimalGenerator(digits)
    }
  }
}

/**
 * Generates a random alphanumeric string of the provided length
 */
data class RandomStringGenerator(val size: Int = 20) : Generator {
  override val type: String
    get() = "RandomString"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to type, "size" to size)
  }

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any {
    return RandomStringUtils.randomAlphanumeric(size)
  }

  companion object {
    fun fromJson(json: JsonValue.Object): RandomStringGenerator {
      val size = if (json["size"].isNumber) {
        json["size"].asNumber()!!.toInt()
      } else {
        logger.warn { "Ignoring invalid value for size: '${json["size"]}'" }
        10
      }
      return RandomStringGenerator(size)
    }
  }
}

/**
 * Generates a random string from the provided regular expression
 */
data class RegexGenerator(val regex: String) : Generator {
  override val type: String
    get() = "Regex"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to type, "regex" to regex)
  }

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any = Generex(regex).random()

  companion object {
    fun fromJson(json: JsonValue.Object) = RegexGenerator(Json.toString(json["regex"]))
  }
}

/**
 * Format of the UUID to generate
 */
enum class UuidFormat {
  /**
   * Simple UUID (e.g 936DA01f9abd4d9d80c702af85c822a8)
   */
  Simple,
  /**
   * lower-case hyphenated (e.g 936da01f-9abd-4d9d-80c7-02af85c822a8)
   */
  LowerCaseHyphenated,
  /**
   * Upper-case hyphenated (e.g 936DA01F-9ABD-4D9D-80C7-02AF85C822A8)
   */
  UpperCaseHyphenated,
  /**
   * URN (e.g. urn:uuid:936da01f-9abd-4d9d-80c7-02af85c822a8)
   */
  Urn;

  override fun toString(): String {
    return when (this) {
      Simple -> "simple"
      LowerCaseHyphenated -> "lower-case-hyphenated"
      UpperCaseHyphenated -> "upper-case-hyphenated"
      Urn -> "URN"
    }
  }

  companion object : KLogging() {
    fun fromString(s: String?): Result<UuidFormat, String> {
      return when(s) {
        "simple" -> Ok(Simple)
        null, "lower-case-hyphenated" -> Ok(LowerCaseHyphenated)
        "upper-case-hyphenated" -> Ok(UpperCaseHyphenated)
        "URN" -> Ok(Urn)
        else -> Err("'$s' is not a valid UUID format")
      }
    }
  }
}

/**
 * Generates a random UUID
 */
data class UuidGenerator @JvmOverloads constructor(val format: UuidFormat? = null) : Generator {
  override val type: String
    get() = "Uuid"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return if (format != null) {
      mapOf("type" to type, "format" to format.toString())
    } else {
      mapOf("type" to type)
    }
  }

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any {
    return if (format != null) {
      when (format) {
        UuidFormat.Simple -> UUID.randomUUID().toString().replace("-", "")
        UuidFormat.LowerCaseHyphenated -> UUID.randomUUID().toString().lowercase()
        UuidFormat.UpperCaseHyphenated -> UUID.randomUUID().toString().uppercase()
        UuidFormat.Urn -> "urn:uuid:" + UUID.randomUUID().toString()
      }
    } else {
      UUID.randomUUID().toString()
    }
  }

  companion object {
    @JvmStatic
    fun fromJson(json: JsonValue.Object): UuidGenerator {
      val format = if (json["format"].isString) UuidFormat.fromString(json["format"].asString()) else null
      return UuidGenerator(format?.get())
    }
  }
}

/**
 * Generates a date value for the provided format. If no format is provided, ISO date format is used. If an expression
 * is given, it will be evaluated to generate the date, otherwise 'today' will be used
 */
data class DateGenerator @JvmOverloads constructor(
  val format: String? = null,
  val expression: String? = null
) : Generator {
  override val type: String
    get() = "Date"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    val map = mutableMapOf("type" to type)
    if (!format.isNullOrEmpty()) {
      map["format"] = this.format
    }
    if (!expression.isNullOrEmpty()) {
      map["expression"] = this.expression
    }
    return map
  }

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any {
    val base = if (context.containsKey("baseDate")) context["baseDate"] as OffsetDateTime
      else OffsetDateTime.now()
    val date = DateExpression.executeDateExpression(base, expression).getOr { base }
    return if (!format.isNullOrEmpty()) {
      date.format(DateTimeFormatter.ofPattern(format))
    } else {
      date.toString()
    }
  }

  companion object {
    fun fromJson(json: JsonValue.Object): DateGenerator {
      val format = if (json["format"].isString) json["format"].asString() else null
      val expression = if (json["expression"].isString) json["expression"].asString() else null
      return DateGenerator(format, expression)
    }
  }
}

/**
 * Generates a time value for the provided format. If no format is provided, ISO time format is used. If an expression
 * is given, it will be evaluated to generate the time, otherwise 'now' will be used
 */
data class TimeGenerator @JvmOverloads constructor(
  val format: String? = null,
  val expression: String? = null
) : Generator {
  override val type: String
    get() = "Time"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    val map = mutableMapOf("type" to type)
    if (!format.isNullOrEmpty()) {
      map["format"] = this.format
    }
    if (!expression.isNullOrEmpty()) {
      map["expression"] = this.expression
    }
    return map
  }

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any {
    val base = if (context.containsKey("baseTime")) context["baseTime"] as OffsetDateTime else OffsetDateTime.now()
    val time = TimeExpression.executeTimeExpression(base, expression).getOr { base }
    return if (!format.isNullOrEmpty()) {
      time.format(DateTimeFormatter.ofPattern(format))
    } else {
      time.toString()
    }
  }

  companion object {
    fun fromJson(json: JsonValue.Object): TimeGenerator {
      val format = if (json["format"].isString) json["format"].asString() else null
      val expression = if (json["expression"].isString) json["expression"].asString() else null
      return TimeGenerator(format, expression)
    }
  }
}

/**
 * Generates a datetime value for the provided format. If no format is provided, ISO format is used. If an expression
 * is given, it will be evaluated to generate the datetime, otherwise 'now' will be used
 */
data class DateTimeGenerator @JvmOverloads constructor(
  val format: String? = null,
  val expression: String? = null
) : Generator {
  override val type: String
    get() = "DateTime"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    val map = mutableMapOf("type" to type)
    if (!format.isNullOrEmpty()) {
      map["format"] = this.format
    }
    if (!expression.isNullOrEmpty()) {
      map["expression"] = this.expression
    }
    return map
  }

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any {
    val base = if (context.containsKey("baseDateTime")) context["baseDateTime"] as OffsetDateTime
      else OffsetDateTime.now()
    val datetime = DateTimeExpression.executeExpression(base, expression).getOr { base }
    return if (!format.isNullOrEmpty()) {
      datetime.toZonedDateTime().format(DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()))
    } else {
      datetime.toString()
    }
  }

  companion object {
    fun fromJson(json: JsonValue.Object): DateTimeGenerator {
      val format = if (json["format"].isString) json["format"].asString() else null
      val expression = if (json["expression"].isString) json["expression"].asString() else null
      return DateTimeGenerator(format, expression)
    }
  }
}

/**
 * Generates a random boolean value
 */
@SuppressWarnings("EqualsWithHashCodeExist")
object RandomBooleanGenerator : Generator {
  override val type: String
    get() = "RandomBoolean"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to type)
  }

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any {
    return ThreadLocalRandom.current().nextBoolean()
  }

  override fun equals(other: Any?) = other is RandomBooleanGenerator

  @Suppress("UNUSED_PARAMETER")
  fun fromJson(json: JsonValue.Object): RandomBooleanGenerator {
    return RandomBooleanGenerator
  }
}

/**
 * Generates a value that is looked up from the provider state context
 */
data class ProviderStateGenerator @JvmOverloads constructor (
  val expression: String,
  val dataType: DataType = DataType.RAW
) : Generator {
  override val type: String
    get() = "ProviderState"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to type, "expression" to expression, "dataType" to dataType.name)
  }

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any? {
    return when (val providerState = context["providerState"]) {
      is Map<*, *> -> {
        val map = providerState as Map<String, Any>
        if (containsExpressions(expression)) {
          parseExpression(expression, dataType, MapValueResolver(map))
        } else {
          map[expression]
        }
      }
      else -> null
    }
  }

  override fun correspondsToMode(mode: GeneratorTestMode) = mode == GeneratorTestMode.Provider

  companion object {
    fun fromJson(json: JsonValue.Object) = ProviderStateGenerator(
      Json.toString(json["expression"]),
      if (json.has("dataType")) DataType.valueOf(Json.toString(json["dataType"])) else DataType.RAW
    )
  }
}

/**
 * Generates a URL with the mock server as the base URL.
 */
data class MockServerURLGenerator(
  val example: String,
  val regex: String
) : Generator {
  override val type: String
    get() = "MockServerURL"

  override fun toMap(pactSpecVersion: PactSpecVersion) = mutableMapOf(
    "type" to type,
    "example" to example,
    "regex" to regex
  )

  override fun correspondsToMode(mode: GeneratorTestMode) = mode == GeneratorTestMode.Consumer

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any? {
    logger.debug { "context = $context" }
    val mockServerDetails = context["mockServer"]
    return if (mockServerDetails != null) {
      if (mockServerDetails is Map<*, *>) {
        val href = mockServerDetails["href"]
        if (href is String && href.isNotEmpty()) {
          try {
            val regex = Regex(regex)
            val match = regex.matchEntire(example)
            if (match != null) {
              if (href.endsWith('/')) {
                href + match.groupValues[1]
              } else {
                href + "/" + match.groupValues[1]
              }
            } else {
              logger.error { "MockServerURL: can not generate a value as the regex did not match the example" }
              null
            }
          } catch (err: PatternSyntaxException) {
            logger.error(err) { "MockServerURL: can not generate a value as the regex is invalid" }
            null
          }
        } else {
          logger.error { "MockServerURL: can not generate a value as there is no mock server URL in the test context" }
          null
        }
      } else {
        logger.error {
          "MockServerURL: can not generate a value as the mock server details in the test context is not an Object"
        }
        null
      }
    } else {
      logger.error { "MockServerURL: can not generate a value as there is no mock server details in the test context" }
      null
    }
  }

  companion object: KLogging() {
    fun fromJson(json: JsonValue.Object): MockServerURLGenerator {
      return MockServerURLGenerator(Json.toString(json["example"]), Json.toString(json["regex"]))
    }
  }
}

object NullGenerator : Generator {
  override val type: String
    get() = "Null"
  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?) = null
  override fun toMap(pactSpecVersion: PactSpecVersion) = emptyMap<String, Any>()
}

data class ArrayContainsGenerator(
  val variants: List<Triple<Int, MatchingRuleCategory, Map<String, Generator>>>
) : Generator {
  override val type: String
    get() = "ArrayContains"

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any? {
    return if (exampleValue is JsonValue.Array) {
      val implementation = context["ArrayContainsJsonGenerator"] as Generator?
      if (implementation != null) {
        context["ArrayContainsVariants"] = variants
        implementation.generate(context, exampleValue)
      } else {
        logger.error { "No ArrayContainsGenerator implementation for JSON found in the test context" }
        null
      }
    } else {
      logger.error { "ArrayContainsGenerator can only be applied to lists" }
      null
    }
  }

  override fun toMap(pactSpecVersion: PactSpecVersion) = emptyMap<String, Any>()
}
