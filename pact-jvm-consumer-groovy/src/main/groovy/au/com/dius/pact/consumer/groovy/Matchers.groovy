package au.com.dius.pact.consumer.groovy

import org.apache.commons.lang3.RandomStringUtils

import java.util.regex.Pattern

class Matchers {

  def regexp(Pattern re, String value = null) {
    regexp(re.toString(), value)
  }

  def regexp(String regexp, String value = null) {
    new RegexpMatcher(values: [regexp, value])
  }

  def hexValue(String value = null) {
    new RegexpMatcher(values: ['[0-9a-fA-F]+', value ?: RandomStringUtils.random(10, "0123456789abcdef")])
  }

  def identifier(def value = null) {
    new TypeMatcher(values: ['type', value ?: RandomStringUtils.randomNumeric(10) as Long])
  }

  def ipAddress(String value = null) {
    new RegexpMatcher(values: ['\\d{1,3}\\.)+\\d{1,3}', value ?: '127.0.0.1'])
  }

  def numeric(Number value = null) {
    new TypeMatcher(values: ['number', value ?: RandomStringUtils.randomNumeric(10) as Long])
  }

  def real(def value = null) {
    new TypeMatcher(values: ['real', value ?: Double.parseDouble(RandomStringUtils.randomNumeric(10)) / 100.0])
  }

  def integer(def value = null) {
    new TypeMatcher(values: ['integer', value ?: RandomStringUtils.randomNumeric(10) as Long])
  }

  def timestamp(String pattern = null, def value = null) {
    new TimestampMatcher(values: value, pattern: pattern)
  }

  def time(String pattern = null, def value = null) {
    new TimeMatcher(values: value, pattern: pattern)
  }

  def date(String pattern = null, def value = null) {
    new DateMatcher(values: value, pattern: pattern)
  }

  def guid(String value = null) {
    new RegexpMatcher(values: ['[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}', value ?: UUID.randomUUID().toString()])
  }

  def string(String value = null) {
    new TypeMatcher(values: ['type', value ?: RandomStringUtils.randomAlphanumeric(10)])
  }

}
