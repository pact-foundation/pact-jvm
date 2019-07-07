package au.com.dius.pact.model.generators

import spock.lang.Ignore
import spock.lang.Specification

import java.time.LocalDateTime

@Ignore
class DateTimeGeneratorSpec extends Specification {

  def 'supports timezones'() {
    expect:
    new DateTimeGenerator('yyyy-MM-dd\'T\'HH:mm:ssZ', null).generate([:]) ==~
      /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[-+]\d+/
  }

  def 'Uses any defined expression to generate the datetime value'() {
    expect:
    new DateTimeGenerator('yyyy-MM-dd\'T\'HH:mm:ssZ', '+ 1 day + 1 hour').generate([:]) == datetime

    where:

    datetime << [LocalDateTime.now().plusDays(1).plusHours(1).format('yyyy-MM-dd\'T\'HH:mm:ss') ]
  }

}
