package au.com.dius.pact.consumer.groovy

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

  def getMatcher() {
    [time: getPattern()]
  }

  def getValue() {
    if (values == null) {
      DateFormatUtils.format(new Date(), getPattern())
    } else {
      values
    }
  }

}
