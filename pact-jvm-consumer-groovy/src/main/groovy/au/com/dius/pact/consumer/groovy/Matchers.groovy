package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.generators.RandomDecimalGenerator
import au.com.dius.pact.model.generators.RandomIntGenerator
import au.com.dius.pact.model.generators.RandomStringGenerator
import org.apache.commons.lang3.time.DateUtils

import java.text.ParseException
import java.util.regex.Pattern

/**
 * Base class for DSL matcher methods
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'ConfusingMethodName'])
class Matchers {

  static final String HEXADECIMAL = '[0-9a-fA-F]+'
  static final String IP_ADDRESS = '(\\d{1,3}\\.)+\\d{1,3}'
  static final String UUID_REGEX = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
  static final int TEN = 10
  static final String TYPE = 'type'
  static final String DECIMAL = 'decimal'
  static final long DATE_2000 = 949323600000L
  static final String INTEGER = 'integer'

  /**
   * Match a regular expression
   * @param re Regular expression pattern
   * @param value Example value, if not provided a random one will be generated
   */
  static regexp(Pattern re, String value = null) {
    regexp(re.toString(), value)
  }

  /**
   * Match a regular expression
   * @param re Regular expression pattern
   * @param value Example value, if not provided a random one will be generated
   */
  static regexp(String regexp, String value = null) {
    if (value && !value.matches(regexp)) {
      throw new InvalidMatcherException("Example \"$value\" does not match regular expression \"$regexp\"")
    }
    new RegexpMatcher(regex: regexp, value: value)
  }

  /**
   * Match a hexadecimal value
   * @param value Example value, if not provided a random one will be generated
   */
  static hexValue(String value = null) {
    if (value && !value.matches(HEXADECIMAL)) {
      throw new InvalidMatcherException("Example \"$value\" is not a hexadecimal value")
    }
    new HexadecimalMatcher(value: value)
  }

  /**
   * Match a numeric identifier (integer)
   * @param value Example value, if not provided a random one will be generated
   */
  static identifier(def value = null) {
    new TypeMatcher(value: value ?: 12345678, type: INTEGER,
      generator: value == null ? new RandomIntGenerator(0, Integer.MAX_VALUE) : null)
  }

  /**
   * Match an IP Address
   * @param value Example value, if not provided 127.0.0.1 will be generated
   */
  static ipAddress(String value = null) {
    if (value && !value.matches(IP_ADDRESS)) {
      throw new InvalidMatcherException("Example \"$value\" is not an ip adress")
    }
    new RegexpMatcher(value: '127.0.0.1', regex: IP_ADDRESS)
  }

  /**
   * Match a numeric value
   * @param value Example value, if not provided a random one will be generated
   */
  static numeric(Number value = null) {
    new TypeMatcher(type: 'number', value: value ?: 100,
      generator: value == null ? new RandomDecimalGenerator(6) : null)
  }

  /**
   * @deprecated Use decimal instead
   */
  @Deprecated
  static real(Number value = null) {
    decimal(value)
  }

  /**
   * Match a decimal value
   * @param value Example value, if not provided a random one will be generated
   */
  static decimal(Number value = null) {
    new TypeMatcher(type: DECIMAL, value: value ?: 100.0,
      generator: value == null ? new RandomDecimalGenerator(6) : null)
  }

  /**
   * Match a integer value
   * @param value Example value, if not provided a random one will be generated
   */
  static integer(Long value = null) {
    new TypeMatcher(type: INTEGER, value: value ?: 100,
      generator: value == null ? new RandomIntGenerator(0, Integer.MAX_VALUE) : null)
  }

  /**
   * Match a timestamp
   * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
   * @param value Example value, if not provided the current date and time will be used
   */
  static timestamp(String pattern = null, def value = null) {
    validateTimeValue(value, pattern)
    new TimestampMatcher(value: value, pattern: pattern)
  }

  private static validateTimeValue(String value, String pattern) {
    if (value && pattern) {
      try {
        DateUtils.parseDateStrictly(value, pattern)
      } catch (ParseException e) {
        throw new InvalidMatcherException("Example \"$value\" does not match pattern \"$pattern\"")
      }
    }
  }

  /**
   * Match a time
   * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
   * @param value Example value, if not provided the current time will be used
   */
  static time(String pattern = null, def value = null) {
    validateTimeValue(value, pattern)
    new TimeMatcher(value: value, pattern: pattern)
  }

  /**
   * Match a date
   * @param pattern Pattern to use to match. If not provided, an ISO pattern will be used.
   * @param value Example value, if not provided the current date will be used
   */

  static date(String pattern = null, def value = null) {
    validateTimeValue(value, pattern)
    new DateMatcher(value: value, pattern: pattern)
  }

  /**
   * Match a globally unique ID (GUID)
   * @param value optional value to use for examples
   * @deprecated use uuid instead
   */
  @SuppressWarnings('ConfusingMethodName')
  @Deprecated
  static guid(String value = null) {
    uuid(value)
  }

  /**
   * Match a universally unique identifier (UUID)
   * @param value optional value to use for examples
   */
  static uuid(String value = null) {
    if (value && !value.matches(UUID_REGEX)) {
      throw new InvalidMatcherException("Example \"$value\" is not a UUID")
    }
    new UuidMatcher(value: value)
  }

  /**
   * Match any string value
   * @param value Example value, if not provided a random one will be generated
   */
  static string(String value = null) {
    if (value) {
      new TypeMatcher(value: value)
    } else {
      new TypeMatcher(value: 'string', generator: new RandomStringGenerator(10))
    }
  }

  /**
   * Array where each element like the following object
   * @param numberExamples Optional number of examples to generate. Defaults to 1.
   */
  static eachLike(Integer numberExamples = 1, def arg) {
    new EachLikeMatcher(value: arg, numberExamples: numberExamples)
  }

  /**
   * Array with maximum size and each element like the following object
   * @param max The maximum size of the array
   * @param numberExamples Optional number of examples to generate. Defaults to 1.
   */
  static maxLike(Integer max, Integer numberExamples = 1, def arg) {
    if (numberExamples > max) {
      throw new InvalidMatcherException("The number of examples you have specified ($numberExamples) is " +
        "greater than the maximum ($max)")
    }
    new MaxLikeMatcher(max: max, value: arg, numberExamples: numberExamples)
  }

  /**
   * Array with minimum size and each element like the following object
   * @param min The minimum size of the array
   * @param numberExamples Optional number of examples to generate. Defaults to 1.
   */
  static minLike(Integer min, Integer numberExamples = 1, def arg) {
    if (numberExamples > 1 && numberExamples < min) {
      throw new InvalidMatcherException("The number of examples you have specified ($numberExamples) is " +
        "less than the minimum ($min)")
    }
    new MinLikeMatcher(min: min, value: arg, numberExamples: numberExamples)
  }

  /**
   * Match Equality
   * @param value Value to match to
   */
  static equalTo(def value) {
    new EqualsMatcher(value: value)
  }

  /**
   * Matches if the string is included in the value
   * @param value String value that must be present
   */
  static includesStr(def value) {
    new IncludeMatcher(value: value?.toString())
  }

}
