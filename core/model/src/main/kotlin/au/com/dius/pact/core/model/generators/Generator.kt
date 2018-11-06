package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.expressions.ExpressionParser.containsExpressions
import au.com.dius.pact.core.support.expressions.ExpressionParser.parseExpression
import au.com.dius.pact.core.support.expressions.MapValueResolver
import com.mifmif.common.regex.Generex
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberFunctions

private val logger = KotlinLogging.logger {}

fun lookupGenerator(generatorMap: Map<String, Any>): Generator? {
  var generator: Generator? = null

  try {
    val generatorClass = Class.forName("au.com.dius.pact.core.model.generators.${generatorMap["type"]}Generator").kotlin
    val fromMap = when {
      generatorClass.companionObject != null ->
        generatorClass.companionObjectInstance to generatorClass.companionObject?.declaredMemberFunctions?.find { it.name == "fromMap" }
      generatorClass.objectInstance != null ->
        generatorClass.objectInstance to generatorClass.declaredMemberFunctions.find { it.name == "fromMap" }
      else -> null
    }
    if (fromMap?.second != null) {
      generator = fromMap.second!!.call(fromMap.first, generatorMap) as Generator?
    } else {
      logger.warn { "Could not invoke generator class 'fromMap' for generator config '$generatorMap'" }
    }
  } catch (e: ClassNotFoundException) {
    logger.warn(e) { "Could not find generator class for generator config '$generatorMap'" }
  }

  return generator
}

interface Generator {
  fun generate(context: Map<String, Any?>): Any?
  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any>
  fun correspondsToMode(mode: GeneratorTestMode): Boolean = true
}

data class RandomIntGenerator(val min: Int, val max: Int) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "RandomInt", "min" to min, "max" to max)
  }

  override fun generate(context: Map<String, Any?>): Any {
    return RandomUtils.nextInt(min, max)
  }

  companion object {
    fun fromMap(map: Map<String, Any>): RandomIntGenerator {
      val min = if (map["min"] is Number) {
        (map["min"] as Number).toInt()
      } else {
        logger.warn { "Ignoring invalid value for min: '${map["min"]}'" }
        0
      }
      val max = if (map["max"] is Number) {
        (map["max"] as Number).toInt()
      } else {
        logger.warn { "Ignoring invalid value for max: '${map["max"]}'" }
        Int.MAX_VALUE
      }
      return RandomIntGenerator(min, max)
    }
  }
}

data class RandomDecimalGenerator(val digits: Int) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "RandomDecimal", "digits" to digits)
  }

  override fun generate(context: Map<String, Any?>): Any = BigDecimal(RandomStringUtils.randomNumeric(digits))

  companion object {
    fun fromMap(map: Map<String, Any>): RandomDecimalGenerator {
      val digits = if (map["digits"] is Number) {
        (map["digits"] as Number).toInt()
      } else {
        logger.warn { "Ignoring invalid value for digits: '${map["digits"]}'" }
        10
      }
      return RandomDecimalGenerator(digits)
    }
  }
}

data class RandomHexadecimalGenerator(val digits: Int) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "RandomHexadecimal", "digits" to digits)
  }

  override fun generate(context: Map<String, Any?>): Any = RandomStringUtils.random(digits, "0123456789abcdef")

  companion object {
    fun fromMap(map: Map<String, Any>): RandomHexadecimalGenerator {
      val digits = if (map["digits"] is Number) {
        (map["digits"] as Number).toInt()
      } else {
        logger.warn { "Ignoring invalid value for digits: '${map["digits"]}'" }
        10
      }
      return RandomHexadecimalGenerator(digits)
    }
  }
}

data class RandomStringGenerator(val size: Int = 20) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "RandomString", "size" to size)
  }

  override fun generate(context: Map<String, Any?>): Any {
    return RandomStringUtils.randomAlphanumeric(size)
  }

  companion object {
    fun fromMap(map: Map<String, Any>): RandomStringGenerator {
      val size = if (map["size"] is Number) {
        (map["size"] as Number).toInt()
      } else {
        logger.warn { "Ignoring invalid value for size: '${map["size"]}'" }
        10
      }
      return RandomStringGenerator(size)
    }
  }
}

data class RegexGenerator(val regex: String) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "Regex", "regex" to regex)
  }

  override fun generate(context: Map<String, Any?>): Any = Generex(regex).random()

  companion object {
    fun fromMap(map: Map<String, Any>) = RegexGenerator(map["regex"]!! as String)
  }
}

object UuidGenerator : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "Uuid")
  }

  override fun generate(context: Map<String, Any?>): Any {
    return UUID.randomUUID().toString()
  }

  @Suppress("UNUSED_PARAMETER")
  fun fromMap(map: Map<String, Any>): UuidGenerator {
    return UuidGenerator
  }
}

data class DateGenerator(val format: String? = null) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    if (format != null) {
      return mapOf("type" to "Date", "format" to this.format)
    }
    return mapOf("type" to "Date")
  }

  override fun generate(context: Map<String, Any?>): Any {
    return if (format != null) {
      OffsetDateTime.now().format(DateTimeFormatter.ofPattern(format))
    } else {
      LocalDate.now().toString()
    }
  }

  companion object {
    fun fromMap(map: Map<String, Any>): DateGenerator {
      return DateGenerator(map["format"] as String?)
    }
  }
}

data class TimeGenerator(val format: String? = null) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    if (format != null) {
      return mapOf("type" to "Time", "format" to this.format)
    }
    return mapOf("type" to "Time")
  }

  override fun generate(context: Map<String, Any?>): Any {
    return if (format != null) {
      OffsetTime.now().format(DateTimeFormatter.ofPattern(format))
    } else {
      LocalTime.now().toString()
    }
  }

  companion object {
    fun fromMap(map: Map<String, Any>): TimeGenerator {
      return TimeGenerator(map["format"] as String?)
    }
  }
}

data class DateTimeGenerator(val format: String? = null) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    if (format != null) {
      return mapOf("type" to "DateTime", "format" to this.format)
    }
    return mapOf("type" to "DateTime")
  }

  override fun generate(context: Map<String, Any?>): Any {
    return if (format != null) {
      ZonedDateTime.now().format(DateTimeFormatter.ofPattern(format))
    } else {
      LocalDateTime.now().toString()
    }
  }

  companion object {
    fun fromMap(map: Map<String, Any>): DateTimeGenerator {
      return DateTimeGenerator(map["format"] as String?)
    }
  }
}

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
  fun fromMap(map: Map<String, Any>): RandomBooleanGenerator {
    return RandomBooleanGenerator
  }
}

data class ProviderStateGenerator(val expression: String) : Generator {
  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    return mapOf("type" to "ProviderState", "expression" to expression)
  }

  override fun generate(context: Map<String, Any?>): Any? {
    val providerState = context["providerState"]
    return when (providerState) {
      is Map<*, *> -> {
        val map = providerState as Map<String, Any>
        if (containsExpressions(expression)) {
          parseExpression(expression, MapValueResolver(map))
        } else {
          map[expression]
        }
      }
      else -> null
    }
  }

  override fun correspondsToMode(mode: GeneratorTestMode) = mode == GeneratorTestMode.Provider

  companion object {
    fun fromMap(map: Map<String, Any>) = ProviderStateGenerator(map["expression"]!! as String)
  }
}
