package au.com.dius.pact.core.model.generators

import com.github.michaelbull.result.getOr
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser.containsExpressions
import au.com.dius.pact.core.support.expressions.ExpressionParser.parseExpression
import au.com.dius.pact.core.support.expressions.MapValueResolver
import au.com.dius.pact.core.support.json.JsonValue
import com.mifmif.common.regex.Generex
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
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
fun lookupGenerator(generatorJson: JsonValue.Object): Generator? {
  var generator: Generator? = null

  try {
    val generatorClass = findGeneratorClass(Json.toString(generatorJson["type"])).kotlin
    val fromJson = when {
      generatorClass.companionObject != null ->
        generatorClass.companionObjectInstance to generatorClass.companionObject?.declaredMemberFunctions?.find {
          it.name == "fromJson" }
      generatorClass.objectInstance != null ->
        generatorClass.objectInstance to generatorClass.declaredMemberFunctions.find { it.name == "fromJson" }
      else -> null
    }
    if (fromJson?.second != null) {
      generator = fromJson.second!!.call(fromJson.first, generatorJson) as Generator?
    } else {
      logger.warn { "Could not invoke generator class 'fromJson' for generator config '$generatorJson'" }
    }
  } catch (e: ClassNotFoundException) {
    logger.warn(e) { "Could not find generator class for generator config '$generatorJson'" }
  }

  return generator
}

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
  fun generate(context: Map<String, Any?>): Any?
  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any>
  fun correspondsToMode(mode: GeneratorTestMode): Boolean = true
}

/**
 * Generates a random integer between a min and max value
 */
data class RandomIntGenerator(val min: Int, val max: Int) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "RandomInt", "min" to min, "max" to max)
  }

  override fun generate(context: Map<String, Any?>): Any {
    return RandomUtils.nextInt(min, max)
  }

  companion object {
    fun fromJson(json: JsonValue.Object): RandomIntGenerator {
      val min = if (json["min"].isNumber) {
        json["min"].asNumber().toInt()
      } else {
        logger.warn { "Ignoring invalid value for min: '${json["min"]}'" }
        0
      }
      val max = if (json["max"].isNumber) {
        json["max"].asNumber().toInt()
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
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "RandomDecimal", "digits" to digits)
  }

  override fun generate(context: Map<String, Any?>): Any {
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
        json["digits"].asNumber().toInt()
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
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "RandomHexadecimal", "digits" to digits)
  }

  override fun generate(context: Map<String, Any?>): Any = RandomStringUtils.random(digits, "0123456789abcdef")

  companion object {
    fun fromJson(json: JsonValue.Object): RandomHexadecimalGenerator {
      val digits = if (json["digits"].isNumber) {
        json["digits"].asNumber().toInt()
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
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "RandomString", "size" to size)
  }

  override fun generate(context: Map<String, Any?>): Any {
    return RandomStringUtils.randomAlphanumeric(size)
  }

  companion object {
    fun fromJson(json: JsonValue.Object): RandomStringGenerator {
      val size = if (json["size"].isNumber) {
        json["size"].asNumber().toInt()
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
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "Regex", "regex" to regex)
  }

  override fun generate(context: Map<String, Any?>): Any = Generex(regex).random()

  companion object {
    fun fromJson(json: JsonValue.Object) = RegexGenerator(Json.toString(json["regex"]))
  }
}

/**
 * Generates a random UUID
 */
object UuidGenerator : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "Uuid")
  }

  override fun generate(context: Map<String, Any?>): Any {
    return UUID.randomUUID().toString()
  }

  @Suppress("UNUSED_PARAMETER")
  fun fromJson(json: JsonValue.Object): UuidGenerator {
    return UuidGenerator
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
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    val map = mutableMapOf("type" to "Date")
    if (!format.isNullOrEmpty()) {
      map["format"] = this.format
    }
    if (!expression.isNullOrEmpty()) {
      map["expression"] = this.expression
    }
    return map
  }

  override fun generate(context: Map<String, Any?>): Any {
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
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    val map = mutableMapOf("type" to "Time")
    if (!format.isNullOrEmpty()) {
      map["format"] = this.format
    }
    if (!expression.isNullOrEmpty()) {
      map["expression"] = this.expression
    }
    return map
  }

  override fun generate(context: Map<String, Any?>): Any {
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
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    val map = mutableMapOf("type" to "DateTime")
    if (!format.isNullOrEmpty()) {
      map["format"] = this.format
    }
    if (!expression.isNullOrEmpty()) {
      map["expression"] = this.expression
    }
    return map
  }

  override fun generate(context: Map<String, Any?>): Any {
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
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "RandomBoolean")
  }

  override fun generate(context: Map<String, Any?>): Any {
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
  val type: DataType = DataType.RAW
) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "ProviderState", "expression" to expression, "dataType" to type.name)
  }

  override fun generate(context: Map<String, Any?>): Any? {
    return when (val providerState = context["providerState"]) {
      is Map<*, *> -> {
        val map = providerState as Map<String, Any>
        if (containsExpressions(expression)) {
          parseExpression(expression, type, MapValueResolver(map))
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
