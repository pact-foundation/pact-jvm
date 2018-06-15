package au.com.dius.pact.model.generators

import au.com.dius.pact.core.model.generators.DateTimeGenerator
import spock.lang.Specification

class DateTimeGeneratorSpec extends Specification {

  def 'supports timezones'() {
    expect:
    new DateTimeGenerator('yyyy-MM-dd\'T\'HH:mm:ssZ').generate(null) ==~
      /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[-+]\d+/
  }

}
