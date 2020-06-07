package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.generators.DateTimeGenerator
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import org.apache.commons.lang3.time.DateFormatUtils

import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Matcher for datetimes
 */
@SuppressWarnings('UnnecessaryGetter')
class DateTimeMatcher extends Matcher {

  String pattern
  String expression = null

  String getPattern() {
    pattern ?: DateFormatUtils.ISO_DATETIME_FORMAT.pattern
  }

  MatchingRule getMatcher() {
    new au.com.dius.pact.core.model.matchingrules.TimestampMatcher(getPattern())
  }

  def getValue() {
    super.@value ?: DateTimeFormatter.ofPattern(getPattern()).withZone(ZoneId.systemDefault()).format(
      new Date(Matchers.DATE_2000).toInstant())
  }

  Generator getGenerator() {
    super.@value == null ? new DateTimeGenerator(getPattern(), expression) : null
  }

}
