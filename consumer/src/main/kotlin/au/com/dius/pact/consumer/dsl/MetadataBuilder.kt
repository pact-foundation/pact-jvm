package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.InvalidMatcherException
import au.com.dius.pact.core.matchers.UrlMatcherSupport
import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.DateTimeGenerator
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator
import au.com.dius.pact.core.model.generators.TimeGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.Category
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.expressions.DataType
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.FastDateFormat
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.TimeZone
import java.util.UUID

open class MetadataBuilder(
  val values: MutableMap<String, Any?> = mutableMapOf(),
  val matchers: Category = Category("metadata"),
  val generators: MutableMap<String, Generator> = mutableMapOf()
) {
  /**
   * Add an entry to the metadata
   */
  fun add(key: String, value: Any?): MetadataBuilder {
    values[key] = value
    return this
  }

  /**
   * Attribute that must be the same type as the example
   * @param name attribute name
   */
  fun like(name: String, example: Any): MetadataBuilder {
    values[name] = example
    matchers.addRule(name, TypeMatcher)
    return this
  }

  /**
   * Attribute that can be any number
   * @param name attribute name
   * @param number example number to use for generated messages
   */
  fun numberType(name: String, number: Number): MetadataBuilder {
    values[name] = number
    matchers.addRule(name, NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER))
    return this
  }

  /**
   * Attribute that must be an integer
   * @param name attribute name
   * @param number example integer value to use for generated messages
   */
  fun integerType(name: String, number: Long): MetadataBuilder {
    values[name] = number
    matchers.addRule(name, NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    return this
  }

  /**
   * Attribute that must be an integer
   * @param name attribute name
   * @param number example integer value to use for generated messages
   */
  fun integerType(name: String, number: Int): MetadataBuilder {
    values[name] = number
    matchers.addRule(name, NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    return this
  }

  /**
   * Attribute that must be a decimalType value
   * @param name attribute name
   * @param number example decimalType value
   */
  fun decimalType(name: String, number: BigDecimal): MetadataBuilder {
    values[name] = number
    matchers.addRule(name, NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    return this
  }

  /**
   * Attribute that must be a decimalType value
   * @param name attribute name
   * @param number example decimalType value
   */
  fun decimalType(name: String, number: Double): MetadataBuilder {
    values[name] = number
    matchers.addRule(name, NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    return this
  }

  /**
   * Attribute that must be a boolean
   * @param name attribute name
   * @param example example boolean to use for generated bodies
   */
  fun booleanType(name: String, example: Boolean): MetadataBuilder {
    values[name] = example
    matchers.addRule(name, TypeMatcher)
    return this
  }

  /**
   * Attribute that must match the regular expression
   * @param name attribute name
   * @param regex regular expression
   * @param value example value to use for generated bodies
   */
  fun matchRegex(name: String, regex: String, value: String): MetadataBuilder {
    if (!value.matches(Regex(regex))) {
      throw InvalidMatcherException("Example \"$value\" does not match regular expression \"$regex\"")
    }
    values[name] = value
    matchers.addRule(name, RegexMatcher(regex))
    return this
  }

  /**
   * Attribute that must be an ISO formatted datetime
   * @param name
   */
  fun datetime(name: String): MetadataBuilder {
    val pattern = DateFormatUtils.ISO_DATETIME_FORMAT.pattern
    generators[name] = DateTimeGenerator(pattern, null)
    values[name] = DateFormatUtils.ISO_DATETIME_FORMAT.format(Date(DslPart.DATE_2000))
    matchers.addRule(name, TimestampMatcher(pattern))
    return this
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   */
  fun datetime(name: String, format: String): MetadataBuilder {
    generators[name] = DateTimeGenerator(format, null)
    val formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault())
    values[name] = formatter.format(Date(DslPart.DATE_2000).toInstant())
    matchers.addRule(name, TimestampMatcher(format))
    return this
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   * @param example example date and time to use for generated bodies
   */
  fun datetime(name: String, format: String, example: Date): MetadataBuilder {
    return datetime(name, format, example, TimeZone.getDefault())
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   * @param example example date and time to use for generated bodies
   * @param timeZone time zone used for formatting of example date and time
   */
  fun datetime(name: String, format: String, example: Date, timeZone: TimeZone): MetadataBuilder {
    val formatter = DateTimeFormatter.ofPattern(format).withZone(timeZone.toZoneId())
    values[name] = formatter.format(example.toInstant())
    matchers.addRule(name, TimestampMatcher(format))
    return this
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   * @param example example date and time to use for generated bodies
   */
  fun datetime(name: String, format: String, example: Instant): MetadataBuilder {
    return datetime(name, format, example, TimeZone.getDefault())
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format timestamp format
   * @param example example date and time to use for generated bodies
   * @param timeZone time zone used for formatting of example date and time
   */
  fun datetime(name: String, format: String, example: Instant, timeZone: TimeZone): MetadataBuilder {
    val formatter = DateTimeFormatter.ofPattern(format).withZone(timeZone.toZoneId())
    values[name] = formatter.format(example)
    matchers.addRule(name, TimestampMatcher(format))
    return this
  }

  /**
   * Attribute that must be formatted as an ISO date
   * @param name attribute name
   */
  fun date(name: String): MetadataBuilder {
    val pattern = DateFormatUtils.ISO_DATE_FORMAT.pattern
    generators[name] = DateGenerator(pattern, null)
    values[name] = DateFormatUtils.ISO_DATE_FORMAT.format(Date(DslPart.DATE_2000))
    matchers.addRule(name, DateMatcher(pattern))
    return this
  }

  /**
   * Attribute that must match the provided date format
   * @param name attribute date
   * @param format date format to match
   */
  fun date(name: String, format: String): MetadataBuilder {
    generators[name] = DateGenerator(format, null)
    val instance = FastDateFormat.getInstance(format)
    values[name] = instance.format(Date(DslPart.DATE_2000))
    matchers.addRule(name, DateMatcher(format))
    return this
  }

  /**
   * Attribute that must match the provided date format
   * @param name attribute date
   * @param format date format to match
   * @param example example date to use for generated values
   */
  fun date(name: String, format: String, example: Date): MetadataBuilder {
    return date(name, format, example, TimeZone.getDefault())
  }

  /**
   * Attribute that must match the provided date format
   * @param name attribute date
   * @param format date format to match
   * @param example example date to use for generated values
   * @param timeZone time zone used for formatting of example date
   */
  fun date(name: String, format: String, example: Date, timeZone: TimeZone): MetadataBuilder {
    val instance = FastDateFormat.getInstance(format, timeZone)
    values[name] = instance.format(example)
    matchers.addRule(name, DateMatcher(format))
    return this
  }

  /**
   * Attribute that must be an ISO formatted time
   * @param name attribute name
   */
  fun time(name: String): MetadataBuilder {
    val pattern = DateFormatUtils.ISO_TIME_FORMAT.pattern
    generators[name] = TimeGenerator(pattern, null)
    values[name] = DateFormatUtils.ISO_TIME_FORMAT.format(Date(DslPart.DATE_2000))
    matchers.addRule(name, TimeMatcher(pattern))
    return this
  }

  /**
   * Attribute that must match the given time format
   * @param name attribute name
   * @param format time format to match
   */
  fun time(name: String, format: String): MetadataBuilder {
    generators[name] = TimeGenerator(format, null)
    val instance = FastDateFormat.getInstance(format)
    values[name] = instance.format(Date(DslPart.DATE_2000))
    matchers.addRule(name, TimeMatcher(format))
    return this
  }

  /**
   * Attribute that must match the given time format
   * @param name attribute name
   * @param format time format to match
   * @param example example time to use for generated bodies
   */
  fun time(name: String, format: String, example: Date): MetadataBuilder {
    return time(name, format, example, TimeZone.getDefault())
  }

  /**
   * Attribute that must match the given time format
   * @param name attribute name
   * @param format time format to match
   * @param example example time to use for generated bodies
   * @param timeZone time zone used for formatting of example time
   */
  fun time(name: String, format: String, example: Date, timeZone: TimeZone): MetadataBuilder {
    val instance = FastDateFormat.getInstance(format, timeZone)
    values[name] = instance.format(example)
    matchers.addRule(name, TimeMatcher(format))
    return this
  }

  /**
   * Attribute that must be an IP4 address
   * @param name attribute name
   */
  fun ipAddress(name: String): MetadataBuilder {
    values[name] = "127.0.0.1"
    matchers.addRule(name, RegexMatcher("(\\d{1,3}\\.)+\\d{1,3}"))
    return this
  }

  /**
   * Attribute that must be encoded as a hexadecimal value
   * @param name attribute name
   */
  fun hexValue(name: String): MetadataBuilder {
    generators[name] = RandomHexadecimalGenerator(10)
    return hexValue(name, "1234a")
  }

  /**
   * Attribute that must be encoded as a hexadecimal value
   * @param name attribute name
   * @param hexValue example value to use for generated bodies
   */
  fun hexValue(name: String, hexValue: String): MetadataBuilder {
    if (!hexValue.matches(Regex(DslPart.HEXADECIMAL))) {
      throw InvalidMatcherException("Example \"$hexValue\" is not a valid hexadecimal value")
    }
    values[name] = hexValue
    matchers.addRule(name, RegexMatcher("[0-9a-fA-F]+"))
    return this
  }

  /**
   * Attribute that must be encoded as an UUID
   * @param name attribute name
   */
  fun uuid(name: String): MetadataBuilder {
    generators[name] = UuidGenerator
    return uuid(name, "e2490de5-5bd3-43d5-b7c4-526e33f71304")
  }

  /**
   * Attribute that must be encoded as an UUID
   * @param name attribute name
   * @param uuid example UUID to use for generated bodies
   */
  fun uuid(name: String, uuid: UUID): MetadataBuilder {
    return uuid(name, uuid.toString())
  }

  /**
   * Attribute that must be encoded as an UUID
   * @param name attribute name
   * @param uuid example UUID to use for generated bodies
   */
  fun uuid(name: String, uuid: String): MetadataBuilder {
    if (!uuid.matches(Regex(DslPart.UUID_REGEX))) {
      throw InvalidMatcherException("Example \"$uuid\" is not a valid UUID")
    }
    values[name] = uuid
    matchers.addRule(name, RegexMatcher(DslPart.UUID_REGEX))
    return this
  }

  /**
   * Attribute that must include the provided string value
   * @param name attribute name
   * @param value Value that must be included
   */
  fun includesStr(name: String, value: String): MetadataBuilder {
    values[name] = value
    matchers.addRule(name, IncludeMatcher(value))
    return this
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions
   * @param name Attribute name
   * @param basePath The base path for the URL (like "http://localhost:8080/") which will be excluded from the matching
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  fun matchUrl(name: String, basePath: String, vararg pathFragments: Any): MetadataBuilder {
    val urlMatcher = UrlMatcherSupport(basePath, listOf(*pathFragments))
    values[name] = urlMatcher.getExampleValue()
    matchers.addRule(name, RegexMatcher(urlMatcher.getRegexExpression()))
    return this
  }

  /**
   * Adds an attribute that will have it's value injected from the provider state
   * @param name Attribute name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to be used in the consumer test
   */
  fun valueFromProviderState(name: String, expression: String, example: Any): MetadataBuilder {
    generators[name] = ProviderStateGenerator(expression, DataType.from(example))
    values[name] = example
    matchers.addRule(name, TypeMatcher)
    return this
  }

  /**
   * Adds a date attribute formatted as an ISO date with the value generated by the date expression
   * @param name Attribute name
   * @param expression Date expression to use to generate the values
   */
  fun dateExpression(name: String, expression: String): MetadataBuilder {
    return dateExpression(name, expression, DateFormatUtils.ISO_DATE_FORMAT.pattern)
  }

  /**
   * Adds a date attribute with the value generated by the date expression
   * @param name Attribute name
   * @param expression Date expression to use to generate the values
   * @param format Date format to use
   */
  fun dateExpression(name: String, expression: String, format: String): MetadataBuilder {
    generators[name] = DateGenerator(format, expression)
    val instance = FastDateFormat.getInstance(format)
    values[name] = instance.format(Date(DslPart.DATE_2000))
    matchers.addRule(name, DateMatcher(format))
    return this
  }

  /**
   * Adds a time attribute formatted as an ISO time with the value generated by the time expression
   * @param name Attribute name
   * @param expression Time expression to use to generate the values
   */
  fun timeExpression(name: String, expression: String): MetadataBuilder {
    return timeExpression(name, expression, DateFormatUtils.ISO_TIME_NO_T_FORMAT.pattern)
  }

  /**
   * Adds a time attribute with the value generated by the time expression
   * @param name Attribute name
   * @param expression Time expression to use to generate the values
   * @param format Time format to use
   */
  fun timeExpression(name: String, expression: String, format: String): MetadataBuilder {
    generators[name] = TimeGenerator(format, expression)
    val instance = FastDateFormat.getInstance(format)
    values[name] = instance.format(Date(DslPart.DATE_2000))
    matchers.addRule(name, TimeMatcher(format))
    return this
  }

  /**
   * Adds a datetime attribute formatted as an ISO datetime with the value generated by the expression
   * @param name Attribute name
   * @param expression Datetime expression to use to generate the values
   */
  fun datetimeExpression(name: String, expression: String): MetadataBuilder {
    return datetimeExpression(name, expression, DateFormatUtils.ISO_DATETIME_FORMAT.pattern)
  }

  /**
   * Adds a datetime attribute with the value generated by the expression
   * @param name Attribute name
   * @param expression Datetime expression to use to generate the values
   * @param format Datetime format to use
   */
  fun datetimeExpression(name: String, expression: String, format: String): MetadataBuilder {
    generators[name] = DateTimeGenerator(format, expression)
    val instance = FastDateFormat.getInstance(format)
    values[name] = instance.format(Date(DslPart.DATE_2000))
    matchers.addRule(name, TimestampMatcher(format))
    return this
  }
}
