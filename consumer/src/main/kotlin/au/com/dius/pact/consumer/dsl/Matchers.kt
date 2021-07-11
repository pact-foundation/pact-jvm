package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.DateTimeGenerator
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.RandomBooleanGenerator
import au.com.dius.pact.core.model.generators.RandomDecimalGenerator
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator
import au.com.dius.pact.core.model.generators.RandomIntGenerator
import au.com.dius.pact.core.model.generators.RandomStringGenerator
import au.com.dius.pact.core.model.generators.RegexGenerator
import au.com.dius.pact.core.model.generators.TimeGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.DateUtils
import java.text.ParseException
import java.util.regex.Pattern

sealed class Matcher(
  value: Any? = null,
  open val matcher: MatchingRule? = null,
  open val generator: Generator? = null
) {
  open val value: Any? = value
    get() = field ?: this.generator?.generate(mutableMapOf(), null)
}

data class RegexpMatcher(val regex: String, override val value: String?) :
  Matcher(value, RegexMatcher(regex, value), if (value == null) RegexGenerator(regex) else null)

data class HexadecimalMatcher(override val value: String?) :
  Matcher(value, RegexMatcher(Matchers.HEXADECIMAL.toString(), value),
    if (value == null) RandomHexadecimalGenerator(10) else null)

data class TypeMatcher(
  override val value: Any?,
  override val matcher: MatchingRule,
  override val generator: Generator?
) : Matcher(value, matcher, generator)

data class TimestampMatcher(
  val pattern: String = DateFormatUtils.ISO_DATETIME_FORMAT.pattern,
  override val value: String?
) : Matcher(
  value,
  au.com.dius.pact.core.model.matchingrules.TimestampMatcher(pattern),
  if (value == null) DateTimeGenerator(pattern) else null
)

data class TimeMatcher(
  val pattern: String = DateFormatUtils.ISO_TIME_FORMAT.pattern,
  override val value: String?
) : Matcher(
  value,
  au.com.dius.pact.core.model.matchingrules.TimeMatcher(pattern),
  if (value == null) TimeGenerator(pattern) else null
)

data class DateMatcher(
  val pattern: String = DateFormatUtils.ISO_DATE_FORMAT.pattern,
  override val value: String?
) : Matcher(
  value,
  au.com.dius.pact.core.model.matchingrules.DateMatcher(pattern),
  if (value == null) DateGenerator(pattern) else null
)

data class UuidMatcher(override val value: String?) :
  Matcher(value, RegexMatcher(Matchers.UUID_REGEX.toString(), value),
    if (value == null) UuidGenerator() else null)

data class EqualsMatcher(override val value: Any?) : Matcher(value, au.com.dius.pact.core.model.matchingrules.EqualsMatcher)

data class IncludeMatcher(override val value: String) : Matcher(value, au.com.dius.pact.core.model.matchingrules.IncludeMatcher(value))

object NullMatcher : Matcher(null, au.com.dius.pact.core.model.matchingrules.NullMatcher)

/**
 * Exception for handling invalid matchers
 */
class InvalidMatcherException(message: String) : RuntimeException(message)

object Matchers {
  val HEXADECIMAL = Regex("[0-9a-fA-F]+")
  val IP_ADDRESS = Regex("(\\d{1,3}\\.)+\\d{1,3}")
  val UUID_REGEX = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

  /**
   * Match a regular expression
   * @param re Regular expression pattern
   * @param value Example value, if not provided a random one will be generated
   */
  @JvmOverloads
  @JvmStatic
  fun regexp(re: Pattern, value: String? = null): Matcher {
    if (!value.isNullOrEmpty() && !value.matches(re.toRegex())) {
      throw InvalidMatcherException("Example \"$value\" does not match regular expression \"$re\"")
    }
    return RegexpMatcher(re.toString(), value)
  }

  /**
   * Match a regular expression
   * @param re Regular expression pattern
   * @param value Example value, if not provided a random one will be generated
   */
  @JvmOverloads
  @JvmStatic
  fun regexp(regexp: String, value: String? = null) = regexp(Pattern.compile(regexp), value)

  /**
   * Match a hexadecimal value
   * @param value Example value, if not provided a random one will be generated
   */
  @JvmOverloads
  @JvmStatic
  fun hexValue(value: String? = null): Matcher {
    if (!value.isNullOrEmpty() && !value.matches(HEXADECIMAL)) {
      throw InvalidMatcherException("Example \"$value\" is not a hexadecimal value")
    }
    return HexadecimalMatcher(value)
  }

  /**
   * Match a numeric identifier (integer)
   * @param value Example value, if not provided a random one will be generated
   */
  @JvmOverloads
  @JvmStatic
  fun identifier(value: Any? = null): Matcher {
    return TypeMatcher(value ?: 12345678, NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER),
      if (value == null) RandomIntGenerator(0, Integer.MAX_VALUE) else null)
  }

  /**
   * Match an IP Address
   * @param value Example value, if not provided 127.0.0.1 will be generated
   */
  @JvmOverloads
  @JvmStatic
  fun ipAddress(value: String? = null): Matcher {
    if (!value.isNullOrEmpty() && !value.matches(IP_ADDRESS)) {
      throw InvalidMatcherException("Example \"$value\" is not an ip adress")
    }
    return RegexpMatcher(IP_ADDRESS.toString(), "127.0.0.1")
  }

  /**
   * Match a numeric value
   * @param value Example value, if not provided a random one will be generated
   */
  @JvmOverloads
  @JvmStatic
  fun numeric(value: Number? = null): Matcher {
    return TypeMatcher(value ?: 100, NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER),
    if (value == null) RandomDecimalGenerator(6) else null)
  }

  /**
   * Match a decimal value
   * @param value Example value, if not provided a random one will be generated
   */
  @JvmOverloads
  @JvmStatic
  fun decimal(value: Number? = null): Matcher {
    return TypeMatcher(value ?: 100.0, NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL),
      if (value == null) RandomDecimalGenerator(6) else null)
  }

  /**
   * Match an integer value
   * @param value Example value, if not provided a random one will be generated
   */
  @JvmOverloads
  @JvmStatic
  fun integer(value: Long? = null): Matcher {
    return TypeMatcher(value ?: 100, NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER),
      if (value == null) RandomIntGenerator(0, Integer.MAX_VALUE) else null)
  }

  /**
   * Match a timestamp
   * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
   * @param value Example value, if not provided the current date and time will be used
   */
  @JvmOverloads
  @JvmStatic
  fun timestamp(pattern: String? = null, value: String? = null): Matcher {
    val pattern = pattern ?: DateFormatUtils.ISO_DATETIME_FORMAT.pattern
    validateTimeValue(value, pattern)
    return TimestampMatcher(pattern, value)
  }

  private fun validateTimeValue(value: String?, pattern: String) {
    if (!value.isNullOrEmpty()) {
      try {
        DateUtils.parseDateStrictly(value, pattern)
      } catch (e: ParseException) {
        throw InvalidMatcherException("Example \"$value\" does not match pattern \"$pattern\"")
      }
    }
  }

  /**
   * Match a time
   * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
   * @param value Example value, if not provided the current time will be used
   */
  @JvmOverloads
  @JvmStatic
  fun time(pattern: String? = null, value: String? = null): Matcher {
    val pattern = pattern ?: DateFormatUtils.ISO_TIME_FORMAT.pattern
    validateTimeValue(value, pattern)
    return TimeMatcher(pattern, value)
  }

  /**
   * Match a date
   * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
   * @param value Example value, if not provided the current date will be used
   */
  @JvmOverloads
  @JvmStatic
  fun date(pattern: String? = null, value: String? = null): Matcher {
    val pattern = pattern ?: DateFormatUtils.ISO_DATE_FORMAT.pattern
    validateTimeValue(value, pattern)
    return DateMatcher(pattern, value)
  }

  /**
   * Match a universally unique identifier (UUID)
   * @param value optional value to use for examples
   */
  @JvmOverloads
  @JvmStatic
  fun uuid(value: String? = null): Matcher {
    if (!value.isNullOrEmpty() && !value.matches(UUID_REGEX)) {
      throw InvalidMatcherException("Example \"$value\" is not a UUID")
    }
    return UuidMatcher(value)
  }

  /**
   * Match any string value
   * @param value Example value, if not provided a random one will be generated
   */
  @JvmOverloads
  @JvmStatic
  fun string(value: String? = null): Matcher {
    return if (value != null) {
      TypeMatcher(value, au.com.dius.pact.core.model.matchingrules.TypeMatcher, null)
    } else {
      TypeMatcher("string", au.com.dius.pact.core.model.matchingrules.TypeMatcher, RandomStringGenerator(10))
    }
  }

  /**
   * Match any boolean
   * @param value Example value, if not provided a random one will be generated
   */
  @JvmOverloads
  @JvmStatic
  fun bool(value: Boolean? = null): Matcher {
    return if (value != null) {
      TypeMatcher(value, au.com.dius.pact.core.model.matchingrules.TypeMatcher, null)
    } else {
      TypeMatcher(true, au.com.dius.pact.core.model.matchingrules.TypeMatcher, RandomBooleanGenerator)
    }
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
   * Matches a null value
   */
  @JvmStatic
  fun nullValue(): Matcher {
    return NullMatcher
  }
}
