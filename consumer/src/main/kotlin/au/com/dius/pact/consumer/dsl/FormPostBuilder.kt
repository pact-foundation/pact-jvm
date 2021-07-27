package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.InvalidMatcherException
import au.com.dius.pact.consumer.dsl.DslPart.Companion.HEXADECIMAL
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.DateTimeGenerator
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator
import au.com.dius.pact.core.model.generators.RegexGenerator
import au.com.dius.pact.core.model.generators.TimeGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.support.expressions.DataType
import com.mifmif.common.regex.Generex
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.FastDateFormat
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.TimeZone
import java.util.UUID

/**
 * Builder for constructing application/x-www-form-urlencoded bodies
 */
class FormPostBuilder(
  val body: MutableMap<String, List<String>> = mutableMapOf(),
  private val contentType: ContentType = ContentType(APPLICATION_FORM_URLENCODED),
  private val matchers: MatchingRuleCategory = MatchingRuleCategory("body"),
  private val generators: Generators = Generators()
) : BodyBuilder {

  /**
   * Attribute that must be have the specified value
   * @param name attribute name
   * @param value string value
   */
  fun stringValue(name: String, value: String): FormPostBuilder {
    body[name] = mutableListOf(value)
    return this
  }

  /**
   * Attribute that must be have the specified values
   * @param name attribute name
   * @param values string values
   */
  fun stringValue(name: String, vararg values: String): FormPostBuilder {
    body[name] = values.toMutableList()
    return this
  }

  /**
   * Attribute that must match the regular expression
   * @param name attribute name
   * @param regex regular expression
   * @param value example value to use for generated bodies
   */
  fun stringMatcher(name: String, regex: String, value: String): FormPostBuilder {
    if (!value.matches(Regex(regex))) {
      throw InvalidMatcherException("Example \"$value\" does not match regular expression \"$regex\"")
    }
    body[name] = listOf(value)
    matchers.addRule(ROOT + name, PM.stringMatcher(regex))
    return this
  }

  /**
   * Attribute that must match the regular expression
   * @param name attribute name
   * @param regex regular expression
   * @param values example values to use for generated bodies
   */
  fun stringMatcher(name: String, regex: String, vararg values: String): FormPostBuilder {
    values.forEach {
      if (!it.matches(Regex(regex))) {
        throw InvalidMatcherException("Example \"$it\" does not match regular expression \"$regex\"")
      }
    }

    body[name] = values.asList()
    matchers.addRule(ROOT + name, PM.stringMatcher(regex))
    return this
  }

  /**
   * Attribute that must match the regular expression
   * @param name attribute name
   * @param regex regular expression
   */
  fun stringMatcher(name: String, regex: String): FormPostBuilder {
    generators.addGenerator(Category.BODY, name, RegexGenerator(regex))
    return stringMatcher(name, regex, Generex(regex).random())
  }

  /**
   * Attribute that must be an ISO formatted datetime
   * @param name
   */
  fun datetime(name: String): FormPostBuilder {
    val pattern = DateFormatUtils.ISO_DATETIME_FORMAT.pattern
    generators.addGenerator(Category.BODY, name, DateTimeGenerator(pattern, null))
    body[name] = listOf(DateFormatUtils.ISO_DATETIME_FORMAT.format(Date(DslPart.DATE_2000)))
    matchers.addRule(ROOT + name, PM.timestamp(pattern))
    return this
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   */
  fun datetime(name: String, format: String): FormPostBuilder {
    generators.addGenerator(Category.BODY, name, DateTimeGenerator(format, null))
    val formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault())
    body[name] = listOf(formatter.format(Date(DslPart.DATE_2000).toInstant()))
    matchers.addRule(ROOT + name, PM.timestamp(format))
    return this
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   * @param example example date and time to use for generated bodies
   */
  fun datetime(name: String, format: String, example: Date): FormPostBuilder {
    return datetime(name, format, example, TimeZone.getDefault())
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   * @param example example date and time to use for generated bodies
   * @param timeZone time zone used for formatting of example date and time
   */
  fun datetime(name: String, format: String, example: Date, timeZone: TimeZone): FormPostBuilder {
    val formatter = DateTimeFormatter.ofPattern(format).withZone(timeZone.toZoneId())
    body[name] = listOf(formatter.format(example.toInstant()))
    matchers.addRule(ROOT + name, PM.timestamp(format))
    return this
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   * @param example example date and time to use for generated bodies
   */
  fun datetime(name: String, format: String, example: Instant): FormPostBuilder {
    return datetime(name, format, example, TimeZone.getDefault())
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format timestamp format
   * @param example example date and time to use for generated bodies
   * @param timeZone time zone used for formatting of example date and time
   */
  fun datetime(name: String, format: String, example: Instant, timeZone: TimeZone): FormPostBuilder {
    val formatter = DateTimeFormatter.ofPattern(format).withZone(timeZone.toZoneId())
    body[name] = listOf(formatter.format(example))
    matchers.addRule(ROOT + name, PM.timestamp(format))
    return this
  }

  /**
   * Attribute that must be formatted as an ISO date
   * @param name attribute name
   */
  fun date(name: String): FormPostBuilder {
    val pattern = DateFormatUtils.ISO_DATE_FORMAT.pattern
    generators.addGenerator(Category.BODY, name, DateGenerator(pattern, null))
    body[name] = listOf(DateFormatUtils.ISO_DATE_FORMAT.format(Date(DslPart.DATE_2000)))
    matchers.addRule(ROOT + name, PM.date(pattern))
    return this
  }

  /**
   * Attribute that must match the provided date format
   * @param name attribute date
   * @param format date format to match
   */
  fun date(name: String, format: String): FormPostBuilder {
    generators.addGenerator(Category.BODY, name, DateGenerator(format, null))
    val instance = FastDateFormat.getInstance(format)
    body[name] = listOf(instance.format(Date(DslPart.DATE_2000)))
    matchers.addRule(ROOT + name, PM.date(format))
    return this
  }

  /**
   * Attribute that must match the provided date format
   * @param name attribute date
   * @param format date format to match
   * @param example example date to use for generated values
   */
  fun date(name: String, format: String, example: Date): FormPostBuilder {
    return date(name, format, example, TimeZone.getDefault())
  }

  /**
   * Attribute that must match the provided date format
   * @param name attribute date
   * @param format date format to match
   * @param example example date to use for generated values
   * @param timeZone time zone used for formatting of example date
   */
  fun date(name: String, format: String, example: Date, timeZone: TimeZone): FormPostBuilder {
    val instance = FastDateFormat.getInstance(format, timeZone)
    body[name] = listOf(instance.format(example))
    matchers.addRule(ROOT + name, PM.date(format))
    return this
  }

  /**
   * Attribute that must be an ISO formatted time
   * @param name attribute name
   */
  fun time(name: String): FormPostBuilder {
    val pattern = DateFormatUtils.ISO_TIME_FORMAT.pattern
    generators.addGenerator(Category.BODY, name, TimeGenerator(pattern, null))
    body[name] = listOf(DateFormatUtils.ISO_TIME_FORMAT.format(Date(DslPart.DATE_2000)))
    matchers.addRule(ROOT + name, PM.time(pattern))
    return this
  }

  /**
   * Attribute that must match the given time format
   * @param name attribute name
   * @param format time format to match
   */
  fun time(name: String, format: String): FormPostBuilder {
    generators.addGenerator(Category.BODY, name, TimeGenerator(format, null))
    val instance = FastDateFormat.getInstance(format)
    body[name] = listOf(instance.format(Date(DslPart.DATE_2000)))
    matchers.addRule(ROOT + name, PM.time(format))
    return this
  }

  /**
   * Attribute that must match the given time format
   * @param name attribute name
   * @param format time format to match
   * @param example example time to use for generated bodies
   */
  fun time(name: String, format: String, example: Date): FormPostBuilder {
    return time(name, format, example, TimeZone.getDefault())
  }

  /**
   * Attribute that must match the given time format
   * @param name attribute name
   * @param format time format to match
   * @param example example time to use for generated bodies
   * @param timeZone time zone used for formatting of example time
   */
  fun time(name: String, format: String, example: Date, timeZone: TimeZone): FormPostBuilder {
    val instance = FastDateFormat.getInstance(format, timeZone)
    body[name] = listOf(instance.format(example))
    matchers.addRule(ROOT + name, PM.time(format))
    return this
  }

  /**
   * Attribute that must be encoded as a hexadecimal value
   * @param name attribute name
   */
  fun hexValue(name: String): FormPostBuilder {
    generators.addGenerator(Category.BODY, name, RandomHexadecimalGenerator(10))
    return hexValue(name, "1234a")
  }

  /**
   * Attribute that must be encoded as a hexadecimal value
   * @param name attribute name
   * @param hexValue example value to use for generated bodies
   */
  fun hexValue(name: String, hexValue: String): FormPostBuilder {
    if (!hexValue.matches(HEXADECIMAL)) {
      throw InvalidMatcherException("Example \"$hexValue\" is not a hexadecimal value")
    }
    body[name] = listOf(hexValue)
    matchers.addRule(ROOT + name, PM.stringMatcher("[0-9a-fA-F]+"))
    return this
  }

  /**
   * Attribute that must be encoded as an UUID
   * @param name attribute name
   */
  fun uuid(name: String): FormPostBuilder {
    generators.addGenerator(Category.BODY, name, UuidGenerator())
    return uuid(name, "e2490de5-5bd3-43d5-b7c4-526e33f71304")
  }

  /**
   * Attribute that must be encoded as an UUID
   * @param name attribute name
   * @param uuid example UUID to use for generated bodies
   */
  fun uuid(name: String, uuid: UUID): FormPostBuilder {
    return uuid(name, uuid.toString())
  }

  /**
   * Attribute that must be encoded as an UUID
   * @param name attribute name
   * @param uuid example UUID to use for generated bodies
   */
  fun uuid(name: String, uuid: String): FormPostBuilder {
    if (!uuid.matches(DslPart.UUID_REGEX)) {
      throw InvalidMatcherException("Example \"$uuid\" is not an UUID")
    }
    body[name] = listOf(uuid)
    matchers.addRule(ROOT + name, PM.stringMatcher(DslPart.UUID_REGEX.pattern))
    return this
  }

  /**
   * Attribute that must include the provided string value
   * @param name attribute name
   * @param value Value that must be included
   */
  fun includesString(name: String, value: String): FormPostBuilder {
    body[name] = listOf(value)
    matchers.addRule(ROOT + name, PM.includesStr(value))
    return this
  }

  /**
   * Adds an attribute that will have it's value injected from the provider state
   * @param name Attribute name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to be used in the consumer test
   */
  fun parameterFromProviderState(name: String, expression: String, example: String): FormPostBuilder {
    generators.addGenerator(Category.BODY, name, ProviderStateGenerator(expression, DataType.STRING))
    body[name] = listOf(example)
    return this
  }

  /**
   * Adds a date attribute formatted as an ISO date with the value generated by the date expression
   * @param name Attribute name
   * @param expression Date expression to use to generate the values
   */
  fun dateExpression(name: String, expression: String): FormPostBuilder {
    return dateExpression(name, expression, DateFormatUtils.ISO_DATE_FORMAT.pattern)
  }

  /**
   * Adds a date attribute with the value generated by the date expression
   * @param name Attribute name
   * @param expression Date expression to use to generate the values
   * @param format Date format to use
   */
  fun dateExpression(name: String, expression: String, format: String): FormPostBuilder {
    generators.addGenerator(Category.BODY, name, DateGenerator(format, expression))
    val instance = FastDateFormat.getInstance(format)
    body[name] = listOf(instance.format(Date(DslPart.DATE_2000)))
    matchers.addRule(ROOT + name, PM.date(format))
    return this
  }

  /**
   * Adds a time attribute formatted as an ISO time with the value generated by the time expression
   * @param name Attribute name
   * @param expression Time expression to use to generate the values
   */
  fun timeExpression(name: String, expression: String): FormPostBuilder {
    return timeExpression(name, expression, DateFormatUtils.ISO_TIME_NO_T_FORMAT.pattern)
  }

  /**
   * Adds a time attribute with the value generated by the time expression
   * @param name Attribute name
   * @param expression Time expression to use to generate the values
   * @param format Time format to use
   */
  fun timeExpression(name: String, expression: String, format: String): FormPostBuilder {
    generators.addGenerator(Category.BODY, name, TimeGenerator(format, expression))
    val instance = FastDateFormat.getInstance(format)
    body[name] = listOf(instance.format(Date(DslPart.DATE_2000)))
    matchers.addRule(ROOT + name, PM.time(format))
    return this
  }

  /**
   * Adds a datetime attribute formatted as an ISO datetime with the value generated by the expression
   * @param name Attribute name
   * @param expression Datetime expression to use to generate the values
   */
  fun datetimeExpression(name: String, expression: String): FormPostBuilder {
    return datetimeExpression(name, expression, DateFormatUtils.ISO_DATETIME_FORMAT.pattern)
  }

  /**
   * Adds a datetime attribute with the value generated by the expression
   * @param name Attribute name
   * @param expression Datetime expression to use to generate the values
   * @param format Datetime format to use
   */
  fun datetimeExpression(name: String, expression: String, format: String): FormPostBuilder {
    generators.addGenerator(Category.BODY, name, DateTimeGenerator(format, expression))
    val instance = FastDateFormat.getInstance(format)
    body[name] = listOf(instance.format(Date(DslPart.DATE_2000)))
    matchers.addRule(ROOT + name, PM.timestamp(format))
    return this
  }

  override fun getMatchers() = matchers
  override fun getGenerators() = generators
  override fun getContentType() = contentType

  override fun buildBody(): ByteArray {
    val charset = contentType.asCharset()
    val charsetStr = charset.toString()
    return body.entries.flatMap { entry ->
      entry.value.map {
        URLEncoder.encode(entry.key, charsetStr) + "=" + URLEncoder.encode(it, charsetStr)
      }
    }.joinToString("&").toByteArray(charset)
  }

  companion object {
    val APPLICATION_FORM_URLENCODED = org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED.toString()
    const val ROOT = "$."
  }
}
