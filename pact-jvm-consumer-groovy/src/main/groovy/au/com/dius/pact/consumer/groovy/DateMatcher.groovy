package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.generators.DateGenerator
import au.com.dius.pact.model.generators.Generator
import au.com.dius.pact.model.matchingrules.MatchingRule
import org.apache.commons.lang3.time.DateFormatUtils

/**
 * Matcher for dates
 *
 */
@SuppressWarnings('UnnecessaryGetter')
class DateMatcher extends Matcher {

  String pattern
  String expression = null

  String getPattern() {
    pattern ?: DateFormatUtils.ISO_DATE_FORMAT.pattern
  }

  MatchingRule getMatcher() {
    new au.com.dius.pact.model.matchingrules.DateMatcher(getPattern())
  }

  Generator getGenerator() {
    super.@value == null ? new DateGenerator(getPattern(), expression) : null
  }

  def getValue() {
    super.@value ?: DateFormatUtils.format(new Date(Matchers.DATE_2000), getPattern())
  }

}
