package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.generators.DateTimeGenerator
import au.com.dius.pact.model.generators.Generator
import au.com.dius.pact.model.matchingrules.MatchingRule
import org.apache.commons.lang3.time.DateFormatUtils

/**
 * Matcher for timestamps
 */
@SuppressWarnings('UnnecessaryGetter')
class TimestampMatcher extends Matcher {

  String pattern
  String expression = null

  String getPattern() {
    pattern ?: DateFormatUtils.ISO_DATETIME_FORMAT.pattern
  }

  MatchingRule getMatcher() {
    new au.com.dius.pact.model.matchingrules.TimestampMatcher(getPattern())
  }

  def getValue() {
    super.@value ?: DateFormatUtils.format(new Date(Matchers.DATE_2000), getPattern())
  }

  Generator getGenerator() {
    super.@value == null ? new DateTimeGenerator(getPattern(), expression) : null
  }

}
