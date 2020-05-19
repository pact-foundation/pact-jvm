package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.support.Json
import spock.lang.Specification

import java.time.LocalDate
import java.time.OffsetDateTime

class DateGeneratorSpec extends Specification {

  def 'supports timezones'() {
    expect:
    new DateGenerator('yyyy-MM-ddZ', null).generate([:]) ==~ /\d{4}-\d{2}-\d{2}[-+]\d+/
  }

  def 'Uses any defined expression to generate the date value'() {
    expect:
    new DateGenerator('yyyy-MM-dd', '+ 1 day').generate([:]) == date

    where:

    date << [ LocalDate.now().plusDays(1).format('yyyy-MM-dd') ]
  }

  def 'Uses json deserialization to work correctly with optional format fields'() {
    given:
    def map = [:]
    def json = Json.INSTANCE.toJson(map).asObject()
    def baseDate = OffsetDateTime.now()

    expect:
    DateGenerator.@Companion.fromJson(json).generate([baseDate: baseDate]) == baseDate.toString()
  }

}
