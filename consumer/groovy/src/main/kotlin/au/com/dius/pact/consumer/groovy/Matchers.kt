package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.groovy.Matchers.Companion.DATE_2000
import au.com.dius.pact.core.matchers.UrlMatcherSupport
import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.DateTimeGenerator
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.MockServerURLGenerator
import au.com.dius.pact.core.model.generators.RandomBooleanGenerator
import au.com.dius.pact.core.model.generators.RandomDecimalGenerator
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator
import au.com.dius.pact.core.model.generators.RandomIntGenerator
import au.com.dius.pact.core.model.generators.RandomStringGenerator
import au.com.dius.pact.core.model.generators.RegexGenerator
import au.com.dius.pact.core.model.generators.TimeGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.BooleanMatcher
import au.com.dius.pact.core.model.matchingrules.HttpStatus
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.support.isNotEmpty
import com.mifmif.common.regex.Generex
import mu.KLogging
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.DateUtils
import java.text.ParseException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.regex.Pattern

const val HEXADECIMAL = "[0-9a-fA-F]+"
const val IP_ADDRESS = "(\\d{1,3}\\.)+\\d{1,3}"
const val UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
const val DECIMAL = "decimal"
const val INTEGER = "integer"

/**
 * Exception for handling invalid matchers
 */
class InvalidMatcherException(message: String) : RuntimeException(message)

/**
 * Marker class for generated values
 */
data class GeneratedValue(val expression: String, val exampleValue: Any?)

/**
 * Base class for matchers
 */
open class Matcher @JvmOverloads constructor(
  open val value: Any? = null,
  open val matcher: MatchingRule? = null,
  open val generator: Generator? = null
)

/**
 * Regular Expression Matcher
 */
class RegexpMatcher @JvmOverloads constructor(
  val regex: String,
  value: String? = null
) : Matcher(value, RegexMatcher(regex, value), if (value == null) RegexGenerator(regex) else null) {
  override val value: Any?
    get() = super.value ?: Generex(regex).random()
}

class HexadecimalMatcher @JvmOverloads constructor(
  value: String? = null
) : Matcher(
  value ?: "1234a",
  RegexMatcher(HEXADECIMAL, value),
  if (value == null) RandomHexadecimalGenerator(10) else null
)

/**
 * Matcher for validating same types
 */
class TypeMatcher @JvmOverloads constructor(
  value: Any? = null,
  val type: String = "type",
  generator: Generator? = null
) : Matcher(
  value,
  when (type) {
    "integer" -> NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)
    "decimal" -> NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL)
    "number" -> NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)
    "boolean" -> BooleanMatcher
    else -> au.com.dius.pact.core.model.matchingrules.TypeMatcher
  },
  generator
)

class DateTimeMatcher @JvmOverloads constructor(
  val pattern: String = DateFormatUtils.ISO_DATETIME_FORMAT.pattern,
  value: String? = null,
  expression: String? = null
) : Matcher(
  value,
  au.com.dius.pact.core.model.matchingrules.TimestampMatcher(pattern),
  if (value == null) DateTimeGenerator(pattern, expression) else null
) {
  override val value: Any?
    get() = super.value ?: DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault()).format(
      Date(DATE_2000).toInstant())
}

class TimeMatcher @JvmOverloads constructor(
  val pattern: String = DateFormatUtils.ISO_TIME_FORMAT.pattern,
  value: String? = null,
  expression: String? = null
) : Matcher(
  value,
  au.com.dius.pact.core.model.matchingrules.TimeMatcher(pattern),
  if (value == null) TimeGenerator(pattern, expression) else null
) {
  override val value: Any?
    get() = super.value ?: DateFormatUtils.format(Date(DATE_2000), pattern)
}

class DateMatcher @JvmOverloads constructor(
  val pattern: String = DateFormatUtils.ISO_DATE_FORMAT.pattern,
  value: String? = null,
  expression: String? = null
) : Matcher(
  value,
  au.com.dius.pact.core.model.matchingrules.DateMatcher(pattern),
  if (value == null) DateGenerator(pattern, expression) else null
) {
  override val value: Any?
    get() = super.value ?: DateFormatUtils.format(Date(DATE_2000), pattern)
}

/**
 * Matcher for universally unique IDs
 */
class UuidMatcher @JvmOverloads constructor(
  value: Any? = null
) : Matcher(
  value ?: "e2490de5-5bd3-43d5-b7c4-526e33f71304",
  RegexMatcher(UUID_REGEX),
  if (value == null) UuidGenerator else null
)

/**
 * Base class for like matchers
 */
open class LikeMatcher @JvmOverloads constructor(
  value: Any? = null,
  val numberExamples: Int = 1,
  matcher: MatchingRule? = null
) : Matcher(value, matcher ?: au.com.dius.pact.core.model.matchingrules.TypeMatcher)

/**
 * Each like matcher for arrays
 */
class EachLikeMatcher @JvmOverloads constructor(
  value: Any? = null,
  numberExamples: Int = 1
) : LikeMatcher(value, numberExamples)

/**
 * Like matcher with a maximum size
 */
class MaxLikeMatcher @JvmOverloads constructor(
  val max: Int,
  value: Any? = null,
  numberExamples: Int = 1
) : LikeMatcher(value, numberExamples, MaxTypeMatcher(max))

/**
 * Like matcher with a minimum size
 */
class MinLikeMatcher @JvmOverloads constructor(
  val min: Int = 1,
  value: Any? = null,
  numberExamples: Int = 1
) : LikeMatcher(value, numberExamples, MinTypeMatcher(min))

/**
 * Like Matcher with a minimum and maximum size
 */
class MinMaxLikeMatcher @JvmOverloads constructor(
  val min: Int,
  val max: Int,
  value: Any? = null,
  numberExamples: Int = 1
) : LikeMatcher(value, numberExamples, MinMaxTypeMatcher(min, max))

/**
 * Matcher to match using equality
 */
class EqualsMatcher(value: Any) : Matcher(value, au.com.dius.pact.core.model.matchingrules.EqualsMatcher)

/**
 * Matcher for string inclusion
 */
class IncludeMatcher(value: String) : Matcher(value, au.com.dius.pact.core.model.matchingrules.IncludeMatcher(value))

/**
 * Matcher that matches if any provided matcher matches
 */
class OrMatcher(example: Any?, val matchers: List<Matcher>) : Matcher(example)

/**
 * Matches if all provided matches match
 */
class AndMatcher(example: Any?, val matchers: List<Matcher>) : Matcher(example)

/**
 * Matcher to match null values
 */
class NullMatcher : Matcher(null, au.com.dius.pact.core.model.matchingrules.NullMatcher)

/**
 * Match a URL by specifying the base and a series of paths.
 */
class UrlMatcher @JvmOverloads constructor(
  val basePath: String,
  val pathFragments: List<Any>,
  private val urlMatcherSupport: UrlMatcherSupport = UrlMatcherSupport(basePath, pathFragments.map {
    if (it is RegexpMatcher) it.matcher!! else it
  })
) : Matcher(
  urlMatcherSupport.getExampleValue(),
  RegexMatcher(urlMatcherSupport.getRegexExpression()),
  if (basePath.isEmpty()) MockServerURLGenerator(urlMatcherSupport.getExampleValue(),
    urlMatcherSupport.getRegexExpression()) else null
) {
  override val value: Any?
    get() = urlMatcherSupport.getExampleValue()
}

/**
 * Array contains matcher for arrays
 */
class ArrayContainsMatcher(
  private val variants: List<Triple<Any, MatchingRuleCategory, Map<String, Generator>>>
) : Matcher(
  buildExample(variants),
  au.com.dius.pact.core.model.matchingrules.ArrayContainsMatcher(buildVariants(variants))
) {
  companion object {
    fun buildExample(variants: List<Triple<Any, MatchingRuleCategory, Map<String, Generator>>>): List<Any?> {
      return variants.map { (value, _, _) ->
        if (value is Matcher) {
          value.value
        } else {
          value
        }
      }
    }

    fun buildVariants(variants: List<Triple<Any, MatchingRuleCategory, Map<String, Generator>>>): List<Triple<Int, MatchingRuleCategory, Map<String, Generator>>> {
      return variants.mapIndexed { index, variant ->
        Triple(index, variant.second, variant.third)
      }
    }
  }
}

/**
 * Matcher for HTTP status codes
 */
class StatusCodeMatcher @JvmOverloads constructor(val status: HttpStatus, value: List<Int> = emptyList())
  : Matcher(value, au.com.dius.pact.core.model.matchingrules.StatusCodeMatcher(status, value)) {
  fun defaultStatus(): Int {
    return when (status) {
      HttpStatus.Information -> 100
      HttpStatus.Success -> 200
      HttpStatus.Redirect -> 300
      HttpStatus.ClientError -> 400
      HttpStatus.ServerError -> 500
      HttpStatus.StatusCodes -> (value as List<Int>).first()
      HttpStatus.NonError -> 200
      HttpStatus.Error -> 400
    }
  }
}

/**
 * Base class for DSL matcher methods
 */
open class Matchers {

  companion object : KLogging() {
    @JvmStatic
    val DATE_2000 = 949323600000L

    /**
     * Match a regular expression
     * @param re Regular expression pattern
     * @param value Example value, if not provided a random one will be generated
     */
    @JvmStatic
    @JvmOverloads
    fun regexp(re: Pattern, value: String? = null) = regexp(re.toString(), value)

    /**
     * Match a regular expression
     * @param re Regular expression pattern
     * @param value Example value, if not provided a random one will be generated
     */
    @JvmStatic
    @JvmOverloads
    fun regexp(regexp: String, value: String? = null): Matcher {
      if (value != null && !value.matches(Regex(regexp))) {
        throw InvalidMatcherException("Example \"$value\" does not match regular expression \"$regexp\"")
      }
      return RegexpMatcher(regexp, value)
    }

    /**
     * Match a hexadecimal value
     * @param value Example value, if not provided a random one will be generated
     */
    @JvmStatic
    @JvmOverloads
    fun hexValue(value: String? = null): Matcher {
      if (value != null && !value.matches(Regex(HEXADECIMAL))) {
        throw InvalidMatcherException("Example \"$value\" is not a hexadecimal value")
      }
      return HexadecimalMatcher(value)
    }

    /**
     * Match a numeric identifier (integer)
     * @param value Example value, if not provided a random one will be generated
     */
    @JvmStatic
    @JvmOverloads
    fun identifier(value: Any? = null): Matcher {
      return TypeMatcher(value ?: 12345678, INTEGER,
        if (value == null) RandomIntGenerator(0, Integer.MAX_VALUE) else null)
    }

    /**
     * Match an IP Address
     * @param value Example value, if not provided 127.0.0.1 will be generated
     */
    @JvmStatic
    @JvmOverloads
    fun ipAddress(value: String? = null): Matcher {
      if (value != null && !value.matches(Regex(IP_ADDRESS))) {
        throw InvalidMatcherException("Example \"$value\" is not an ip address")
      }
      return RegexpMatcher(IP_ADDRESS, value ?: "127.0.0.1")
    }

    /**
     * Match a numeric value
     * @param value Example value, if not provided a random one will be generated
     */
    @JvmStatic
    @JvmOverloads
    fun numeric(value: Number? = null): Matcher {
      return TypeMatcher(value ?: 100, "number", if (value == null) RandomDecimalGenerator(6) else null)
    }

    /**
     * Match a decimal value
     * @param value Example value, if not provided a random one will be generated
     */
    @JvmStatic
    @JvmOverloads
    fun decimal(value: Number? = null): Matcher {
      return TypeMatcher(value ?: 100.0, DECIMAL, if (value == null) RandomDecimalGenerator(6) else null)
    }

    /**
     * Match a integer value
     * @param value Example value, if not provided a random one will be generated
     */
    @JvmStatic
    @JvmOverloads
    fun integer(value: Long? = null): Matcher {
      return TypeMatcher(value ?: 100, INTEGER,
        if (value == null) RandomIntGenerator(0, Integer.MAX_VALUE) else null)
    }

    /**
     * Match a datetime
     * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
     * @param value Example value, if not provided the current date and time will be used
     */
    @JvmStatic
    @JvmOverloads
    fun datetime(pattern: String = DateFormatUtils.ISO_DATETIME_FORMAT.pattern, value: String? = null): Matcher {
      validateDateTimeValue(value, pattern)
      return DateTimeMatcher(pattern, value)
    }

    /**
     * Match a datetime generated from an expression
     * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
     * @param expression Expression to use to generate the datetime
     */
    fun datetimeExpression(expression: String, pattern: String = DateFormatUtils.ISO_DATETIME_FORMAT.pattern): Matcher {
      return DateTimeMatcher(pattern, null, expression)
    }

    private fun validateTimeValue(value: String?, pattern: String?) {
      if (value.isNotEmpty() && pattern.isNotEmpty()) {
        try {
          DateUtils.parseDateStrictly(value, pattern)
        } catch (e: ParseException) {
          throw InvalidMatcherException("Example \"$value\" does not match pattern \"$pattern\"")
        }
      }
    }

    private fun validateDateTimeValue(value: String?, pattern: String?) {
      if (value.isNotEmpty() && pattern.isNotEmpty()) {
        try {
          ZonedDateTime.parse(value, DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault()))
        } catch (e: DateTimeParseException) {
          logger.error(e) { "Example \"$value\" does not match pattern \"$pattern\"" }
          throw InvalidMatcherException("Example \"$value\" does not match pattern \"$pattern\"")
        }
      }
    }

    /**
     * Match a time
     * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
     * @param value Example value, if not provided the current time will be used
     */
    @JvmStatic
    @JvmOverloads
    fun time(pattern: String = DateFormatUtils.ISO_TIME_FORMAT.pattern, value: String? = null): Matcher {
      validateTimeValue(value, pattern)
      return TimeMatcher(pattern, value)
    }

    /**
     * Match a time generated from an expression
     * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
     * @param expression Expression to use to generate the time
     */
    @JvmStatic
    @JvmOverloads
    fun timeExpression(expression: String, pattern: String = DateFormatUtils.ISO_TIME_FORMAT.pattern): Matcher {
      return TimeMatcher(pattern, null, expression)
    }

    /**
     * Match a date
     * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
     * @param value Example value, if not provided the current date will be used
     */
    @JvmStatic
    @JvmOverloads
    fun date(pattern: String = DateFormatUtils.ISO_DATE_FORMAT.pattern, value: String? = null): Matcher {
      validateTimeValue(value, pattern)
      return DateMatcher(pattern, value)
    }

    /**
     * Match a date generated from an expression
     * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
     * @param expression Expression to use to generate the date
     */
    @JvmStatic
    @JvmOverloads
    fun dateExpression(expression: String, pattern: String = DateFormatUtils.ISO_DATE_FORMAT.pattern): Matcher {
      return DateMatcher(pattern, null, expression)
    }

    /**
     * Match a universally unique identifier (UUID)
     * @param value optional value to use for examples
     */
    @JvmStatic
    @JvmOverloads
    fun uuid(value: String? = null): Matcher {
      if (value != null && !value.matches(Regex(UUID_REGEX))) {
        throw InvalidMatcherException("Example \"$value\" is not a UUID")
      }
      return UuidMatcher(value)
    }

    /**
     * Match any string value
     * @param value Example value, if not provided a random one will be generated
     */
    @JvmStatic
    @JvmOverloads
    fun string(value: String? = null): Matcher {
      return if (value != null) {
        TypeMatcher(value)
      } else {
        TypeMatcher("string", generator = RandomStringGenerator(10))
      }
    }

    /**
     * Match any boolean
     * @param value Example value, if not provided a random one will be generated
     */
    @JvmStatic
    @JvmOverloads
    fun bool(value: Boolean? = null): Matcher {
      return if (value != null) {
        TypeMatcher(value, "boolean")
      } else {
        TypeMatcher(true, "boolean", RandomBooleanGenerator)
      }
    }

    /**
     * Array where each element like the following object
     * @param numberExamples Optional number of examples to generate. Defaults to 1.
     */
    @JvmStatic
    @JvmOverloads
    fun eachLike(numberExamples: Int = 1, arg: Any): Matcher {
      return EachLikeMatcher(arg, numberExamples)
    }

    /**
     * Array with maximum size and each element like the following object
     * @param max The maximum size of the array
     * @param numberExamples Optional number of examples to generate. Defaults to 1.
     */
    @JvmStatic
    @JvmOverloads
    fun maxLike(max: Int, numberExamples: Int = 1, arg: Any): Matcher {
      if (numberExamples > max) {
        throw InvalidMatcherException("The number of examples you have specified ($numberExamples) is " +
          "greater than the maximum ($max)")
      }
      return MaxLikeMatcher(max, arg, numberExamples)
    }

    /**
     * Array with minimum size and each element like the following object
     * @param min The minimum size of the array
     * @param numberExamples Optional number of examples to generate. Defaults to 1.
     */
    @JvmStatic
    @JvmOverloads
    fun minLike(min: Int, numberExamples: Int = 1, arg: Any): Matcher {
      if (numberExamples in 2 until min) {
        throw InvalidMatcherException("The number of examples you have specified ($numberExamples) is " +
          "less than the minimum ($min)")
      }
      return MinLikeMatcher(min, arg, numberExamples)
    }

    /**
     * Array with minimum and maximum size and each element like the following object
     * @param min The minimum size of the array
     * @param max The maximum size of the array
     * @param numberExamples Optional number of examples to generate. Defaults to 1.
     */
    @JvmStatic
    @JvmOverloads
    fun minMaxLike(min: Int, max: Int, numberExamples: Int = 1, arg: Any): Matcher {
      if (min > max) {
        throw InvalidMatcherException("The minimum you have specified ($min) is " +
          "greater than the maximum ($max)")
      } else if (numberExamples > 1 && numberExamples < min) {
        throw InvalidMatcherException("The number of examples you have specified ($numberExamples) is " +
          "less than the minimum ($min)")
      } else if (numberExamples > 1 && numberExamples > max) {
        throw InvalidMatcherException("The number of examples you have specified ($numberExamples) is " +
          "greater than the maximum ($max)")
      }
      return MinMaxLikeMatcher(min, max, arg, numberExamples)
    }

    /**
     * Match Equality
     * @param value Value to match to
     */
    @JvmStatic
    fun equalTo(value: Any): Matcher {
      return EqualsMatcher(value)
    }

    /**
     * Matches if the string is included in the value
     * @param value String value that must be present
     */
    @JvmStatic
    fun includesStr(value: String): Matcher {
      return IncludeMatcher(value)
    }

    /**
     * Matches if any of the provided matches match
     * @param example Example value to use
     */
    @JvmStatic
    fun or(example: Any?, vararg values: Any): Matcher {
      return OrMatcher(example, values.map {
        if (it is Matcher) {
          it
        } else {
          EqualsMatcher(it)
        }
      })
    }

    /**
     * Matches if all of the provided matches match
     * @param example Example value to use
     */
    @JvmStatic
    fun and(example: Any?, vararg values: Any): Matcher {
      return AndMatcher(example, values.map {
        if (it is Matcher) {
          it
        } else {
          EqualsMatcher(it)
        }
      })
    }

    /**
     * Matches a null value
     */
    @JvmStatic
    fun nullValue(): Matcher {
      return NullMatcher()
    }

    /**
     * Matches a URL composed of a base path and a list of path fragments
     */
    @JvmStatic
    fun url(basePath: String, vararg pathFragments: Any): Matcher {
      return UrlMatcher(basePath, pathFragments.toList())
    }

    /**
     * Array of arrays where each element like the following object
     * @param numberExamples Optional number of examples to generate. Defaults to 1.
     */
    @JvmStatic
    @JvmOverloads
    fun eachArrayLike(numberExamples: Int = 1, arg: Any): Matcher {
      return EachLikeMatcher(EachLikeMatcher(arg, numberExamples), numberExamples)
    }

    /**
     * Match any HTTP Information response status (100-199)
     */
    @JvmStatic
    fun informationStatus() = StatusCodeMatcher(HttpStatus.Information)

    /**
     * Match any HTTP success response status (200-299)
     */
    @JvmStatic
    fun successStatus() = StatusCodeMatcher(HttpStatus.Success)

    /**
     * Match any HTTP redirect response status (300-399)
     */
    @JvmStatic
    fun redirectStatus() = StatusCodeMatcher(HttpStatus.Redirect)

    /**
     * Match any HTTP client error response status (400-499)
     */
    @JvmStatic
    fun clientErrorStatus() = StatusCodeMatcher(HttpStatus.ClientError)

    /**
     * Match any HTTP server error response status (500-599)
     */
    @JvmStatic
    fun serverErrorStatus() = StatusCodeMatcher(HttpStatus.ServerError)

    /**
     * Match any HTTP non-error response status (< 400)
     */
    @JvmStatic
    fun nonErrorStatus() = StatusCodeMatcher(HttpStatus.NonError)

    /**
     * Match any HTTP error response status (>= 400)
     */
    @JvmStatic
    fun errorStatus() = StatusCodeMatcher(HttpStatus.Error)

    /**
     * Match any HTTP status code in the provided list
     */
    @JvmStatic
    fun statusCodes(statusCodes: List<Int>) = StatusCodeMatcher(HttpStatus.StatusCodes, statusCodes)
  }
}
