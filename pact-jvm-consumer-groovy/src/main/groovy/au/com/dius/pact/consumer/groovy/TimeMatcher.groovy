package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.generators.Generator
import au.com.dius.pact.model.generators.TimeGenerator
import au.com.dius.pact.model.matchingrules.MatchingRule
import org.apache.commons.lang3.time.DateFormatUtils

/**
 * Matcher for time values
 */
@SuppressWarnings('UnnecessaryGetter')
class TimeMatcher extends Matcher {

  String pattern

  String getPattern() {
    pattern ?: DateFormatUtils.ISO_TIME_FORMAT.pattern
  }

  MatchingRule getMatcher() {
    new au.com.dius.pact.model.matchingrules.TimeMatcher(getPattern())
  }

  Generator getGenerator() {
    super.@value == null ? new TimeGenerator(getPattern()) : null
  }

  def getValue() {
    super.@value ?: DateFormatUtils.format(new Date(Matchers.DATE_2000), getPattern())
  }

}
