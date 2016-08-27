package au.com.dius.pact.consumer.groovy

import org.apache.commons.lang3.time.DateFormatUtils

/**
 * Matcher for dates
 */
@SuppressWarnings('UnnecessaryGetter')
class DateMatcher extends Matcher {

  String pattern

  String getPattern() {
    pattern ?: DateFormatUtils.ISO_DATE_FORMAT.pattern
  }

  def getMatcher() {
    new au.com.dius.pact.model.matchingrules.DateMatcher(getPattern())
  }

  def getValue() {
    if (values == null) {
      DateFormatUtils.format(new Date(), getPattern())
    } else {
      values
    }
  }

}
