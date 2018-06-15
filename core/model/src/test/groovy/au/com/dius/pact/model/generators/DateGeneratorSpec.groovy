package au.com.dius.pact.model.generators

import au.com.dius.pact.core.model.generators.DateGenerator
import spock.lang.Specification

class DateGeneratorSpec extends Specification {

  def 'supports timezones'() {
    expect:
    new DateGenerator('yyyy-MM-ddZ').generate(null) ==~ /\d{4}-\d{2}-\d{2}[-+]\d+/
  }

}
