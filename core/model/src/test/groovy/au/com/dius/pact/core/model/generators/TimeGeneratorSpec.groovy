package au.com.dius.pact.core.model.generators

import spock.lang.Specification

import java.time.OffsetTime

class TimeGeneratorSpec extends Specification {

  def 'supports timezones'() {
    expect:
    new TimeGenerator('HH:mm:ssZ', null).generate([:]) ==~ /\d{2}:\d{2}:\d{2}[-+]\d+/
  }

  def 'Uses any defined expression to generate the time value'() {
    expect:
    new TimeGenerator('HH:mm:ss', '+ 1 hour').generate([baseTime: base]) == time

    where:
    base = OffsetTime.now()
    time = base.plusHours(1).format('HH:mm:ss')
  }

}
