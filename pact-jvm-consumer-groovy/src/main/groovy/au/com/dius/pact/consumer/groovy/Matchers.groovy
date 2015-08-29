package au.com.dius.pact.consumer.groovy

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.time.DateUtils

import java.text.ParseException
import java.util.regex.Pattern

/**
 * Base class for DSL matcher methods
 */
class Matchers {

  static final String HEXADECIMAL = '[0-9a-fA-F]+'
  static final String IP_ADDRESS = '(\\d{1,3}\\.)+\\d{1,3}'
  static final String UUID_REGEX = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
  private static final int TEN = 10
  public static final String TYPE = 'type'

  def regexp(Pattern re, String value = null) {
    regexp(re.toString(), value)
  }

  def regexp(String regexp, String value = null) {
    if (value && !value.matches(regexp)) {
      throw new InvalidMatcherException("Example \"$value\" does not match regular expression \"$regexp\"")
    }
    new RegexpMatcher(values: [regexp, value])
  }

  def hexValue(String value = null) {
    if (value && !value.matches(HEXADECIMAL)) {
      throw new InvalidMatcherException("Example \"$value\" is not a hexadecimal value")
    }
    new RegexpMatcher(values: [HEXADECIMAL, value ?: RandomStringUtils.random(TEN, '0123456789abcdef')])
  }

  def identifier(def value = null) {
    new TypeMatcher(values: [TYPE, value ?: RandomStringUtils.randomNumeric(TEN) as Long])
  }

  def ipAddress(String value = null) {
    if (value && !value.matches(IP_ADDRESS)) {
      throw new InvalidMatcherException("Example \"$value\" is not an ip adress")
    }
    new RegexpMatcher(values: [IP_ADDRESS, value ?: '127.0.0.1'])
  }

  def numeric(Number value = null) {
    new TypeMatcher(values: ['number', value ?: RandomStringUtils.randomNumeric(TEN) as Long])
  }

  def real(Number value = null) {
    new TypeMatcher(values: ['real', value ?: Double.parseDouble(RandomStringUtils.randomNumeric(TEN)) / 100.0])
  }

  def integer(Integer value = null) {
    new TypeMatcher(values: ['integer', value ?: RandomStringUtils.randomNumeric(TEN) as Long])
  }

  def timestamp(String pattern = null, def value = null) {
    validateTimeValue(value, pattern)
    new TimestampMatcher(values: value, pattern: pattern)
  }

  private validateTimeValue(String value, String pattern) {
    if (value && pattern) {
      try {
        DateUtils.parseDateStrictly(value, pattern)
      } catch (ParseException e) {
        throw new InvalidMatcherException("Example \"$value\" does not match pattern \"$pattern\"")
      }
    }
  }

  def time(String pattern = null, def value = null) {
    validateTimeValue(value, pattern)
    new TimeMatcher(values: value, pattern: pattern)
  }

  def date(String pattern = null, def value = null) {
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
  def guid(String value = null) {
    uuid(value)
  }

  /**
   * Match a universally unique identifier (UUID)
   * @param value optional value to use for examples
   */
  def uuid(String value = null) {
    if (value && !value.matches(UUID_REGEX)) {
      throw new InvalidMatcherException("Example \"$value\" is not a UUID")
    }
    new RegexpMatcher(values: [UUID_REGEX, value ?: UUID.randomUUID().toString()])
  }

  def string(String value = null) {
    new TypeMatcher(values: [TYPE, value ?: RandomStringUtils.randomAlphanumeric(TEN)])
  }

  /**
   * Array with maximum size and each element like the following object
   */
  def maxLike(Integer max, Closure closure) {
    new MaxLikeMatcher(values: [max, closure])
  }

  /**
   * Array with minimum size and each element like the following object
   */
  def minLike(Integer min, Closure closure) {
    new MinLikeMatcher(values: [min, closure])
  }

  /**
   * Array where each element is like the following object
   */
  def eachLike(Closure closure) {
    new EachLikeMatcher(values: [null,  closure])
  }

}
