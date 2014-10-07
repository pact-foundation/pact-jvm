package au.com.dius.pact.consumer.groovy

import org.apache.commons.lang3.time.DateFormatUtils

class TimestampMatcher extends Matcher {

  def getMatcher() {
    [match: 'timestamp']
  }

  def getValue() {
    if (values == null) {
      DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date())
    } else {
      values
    }
  }

}
