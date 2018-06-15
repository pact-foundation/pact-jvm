package au.com.dius.pact.model.generators

import au.com.dius.pact.core.model.generators.TimeGenerator
import spock.lang.Specification

class TimeGeneratorSpec extends Specification {

  def 'supports timezones'() {
    expect:
    new TimeGenerator('HH:mm:ssZ').generate(null) ==~ /\d{2}:\d{2}:\d{2}[-+]\d+/
  }

}
