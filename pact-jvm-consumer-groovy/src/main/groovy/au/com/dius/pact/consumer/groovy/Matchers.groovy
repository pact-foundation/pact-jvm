package au.com.dius.pact.consumer.groovy

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.time.DateUtils

import java.text.ParseException
import java.util.regex.Pattern

class Matchers {

  static final String HEXADECIMAL = '[0-9a-fA-F]+'
  static final String IP_ADDRESS = '(\\d{1,3}\\.)+\\d{1,3}'
  static final String GUID = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'

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
    new RegexpMatcher(values: [HEXADECIMAL, value ?: RandomStringUtils.random(10, "0123456789abcdef")])
  }

  def identifier(def value = null) {
    new TypeMatcher(values: ['type', value ?: RandomStringUtils.randomNumeric(10) as Long])
  }

  def ipAddress(String value = null) {
    if (value && !value.matches(IP_ADDRESS)) {
      throw new InvalidMatcherException("Example \"$value\" is not an ip adress")
    }
    new RegexpMatcher(values: [IP_ADDRESS, value ?: '127.0.0.1'])
  }

  def numeric(Number value = null) {
    new TypeMatcher(values: ['number', value ?: RandomStringUtils.randomNumeric(10) as Long])
  }

  def real(Number value = null) {
    new TypeMatcher(values: ['real', value ?: Double.parseDouble(RandomStringUtils.randomNumeric(10)) / 100.0])
  }

  def integer(Integer value = null) {
    new TypeMatcher(values: ['integer', value ?: RandomStringUtils.randomNumeric(10) as Long])
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

  def guid(String value = null) {
    if (value && !value.matches(GUID)) {
      throw new InvalidMatcherException("Example \"$value\" is not a GUID")
    }
    new RegexpMatcher(values: [GUID, value ?: UUID.randomUUID().toString()])
  }

  def string(String value = null) {
    new TypeMatcher(values: ['type', value ?: RandomStringUtils.randomAlphanumeric(10)])
  }

}
