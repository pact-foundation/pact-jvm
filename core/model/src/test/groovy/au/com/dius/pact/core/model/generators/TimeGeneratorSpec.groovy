package au.com.dius.pact.core.model.generators

import spock.lang.Specification

class TimeGeneratorSpec extends Specification {

  def 'supports timezones'() {
    expect:
    new TimeGenerator('HH:mm:ssZ').generate([:]) ==~ /\d{2}:\d{2}:\d{2}[-+]\d+/
  }

}
