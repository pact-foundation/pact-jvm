package au.com.dius.pact.consumer.groovy

import org.apache.commons.lang3.RandomStringUtils
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
  public static final int TEN = 10
  public static final String TYPE = 'type'
  public static final String DECIMAL = 'decimal'

  static regexp(Pattern re, String value = null) {
    regexp(re.toString(), value)
  }

  static regexp(String regexp, String value = null) {
    if (value && !value.matches(regexp)) {
      throw new InvalidMatcherException("Example \"$value\" does not match regular expression \"$regexp\"")
    }
    new RegexpMatcher(values: [regexp, value])
  }

  static hexValue(String value = null) {
    if (value && !value.matches(HEXADECIMAL)) {
      throw new InvalidMatcherException("Example \"$value\" is not a hexadecimal value")
    }
    new RegexpMatcher(values: [HEXADECIMAL, value ?: RandomStringUtils.random(TEN, '0123456789abcdef')])
  }

  static identifier(def value = null) {
    new TypeMatcher(values: [TYPE, value ?: RandomStringUtils.randomNumeric(TEN) as Long])
  }

  static ipAddress(String value = null) {
    if (value && !value.matches(IP_ADDRESS)) {
      throw new InvalidMatcherException("Example \"$value\" is not an ip adress")
    }
    new RegexpMatcher(values: [IP_ADDRESS, value ?: '127.0.0.1'])
  }

  static numeric(Number value = null) {
    new TypeMatcher(values: ['number', value ?: RandomStringUtils.randomNumeric(TEN) as Long])
  }

  /**
   * @deprecated Use decimal instead
   */
  @Deprecated
  static real(Number value = null) {
    new TypeMatcher(values: [DECIMAL, value ?: (RandomStringUtils.randomNumeric(TEN) as BigDecimal) / 100.0])
  }

  static decimal(Number value = null) {
    new TypeMatcher(values: [DECIMAL, value ?: (RandomStringUtils.randomNumeric(TEN) as BigDecimal) / 100.0])
  }

  static integer(Long value = null) {
    new TypeMatcher(values: ['integer', value ?: RandomStringUtils.randomNumeric(TEN) as Long])
  }

  static timestamp(String pattern = null, def value = null) {
    validateTimeValue(value, pattern)
    new TimestampMatcher(values: value, pattern: pattern)
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

  static time(String pattern = null, def value = null) {
    validateTimeValue(value, pattern)
    new TimeMatcher(values: value, pattern: pattern)
  }

  static date(String pattern = null, def value = null) {
    validateTimeValue(value, pattern)
    new DateMatcher(values: value, pattern: pattern)
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
    new RegexpMatcher(values: [UUID_REGEX, value ?: UUID.randomUUID().toString()])
  }

  static string(String value = null) {
    new TypeMatcher(values: [TYPE, value ?: RandomStringUtils.randomAlphanumeric(TEN)])
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
    new MaxLikeMatcher(values: [max, arg], numberExamples: numberExamples)
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
    new MinLikeMatcher(values: [min, arg], numberExamples: numberExamples)
  }

  /**
   * Array where each element is like the following object
   * @param numberExamples Optional number of examples to generate. Defaults to 1.
   */
  static eachLike(Integer numberExamples = 1, def arg) {
    new EachLikeMatcher(values: [null,  arg], numberExamples: numberExamples)
  }

}
