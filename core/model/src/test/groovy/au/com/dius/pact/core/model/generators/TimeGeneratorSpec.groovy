package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.support.Json
import com.google.gson.JsonObject
import spock.lang.Specification

import java.time.OffsetDateTime

class TimeGeneratorSpec extends Specification {

  def 'supports timezones'() {
    expect:
    new TimeGenerator('HH:mm:ssZ', null).generate([:]) ==~ /\d{2}:\d{2}:\d{2}[-+]\d+/
  }

  def 'Uses any defined expression to generate the time value'() {
    expect:
    new TimeGenerator('HH:mm:ss', '+ 1 hour').generate([baseTime: base]) == time

    where:
    base = OffsetDateTime.now()
    time = base.plusHours(1).format('HH:mm:ss')
  }

  def 'Uses json deserialization to work correctly with optional format fields'() {
    given:
    def json = Json.INSTANCE.toJson([:])
    def baseTime = OffsetDateTime.now()

    expect:
    TimeGenerator.@Companion.fromJson((JsonObject) json).generate([baseTime: baseTime]) == baseTime.toString()
  }
}
