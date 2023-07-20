package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.support.Json
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TimeGeneratorSpec extends Specification {

  def 'supports timezones'() {
    expect:
    new TimeGenerator('HH:mm:ssZ', null).generate([:], null) ==~ /\d{2}:\d{2}:\d{2}[-+]\d+/
  }

  def 'Uses any defined expression to generate the time value'() {
    expect:
    new TimeGenerator('HH:mm:ss', '+ 1 hour').generate([baseTime: base], null) == time

    where:
    base = OffsetDateTime.now()
    time = base.plusHours(1).format('HH:mm:ss')
  }

  def 'Uses json deserialization to work correctly with optional format fields'() {
    given:
    def json = Json.INSTANCE.toJson([:]).asObject()
    def baseTime = LocalTime.now()
    def base = baseTime.atOffset(ZoneOffset.ofHours(11)).atDate(LocalDate.now())

    expect:
    TimeGenerator.@Companion.fromJson(json).generate([baseTime: base], null) == baseTime.toString()
  }
}
