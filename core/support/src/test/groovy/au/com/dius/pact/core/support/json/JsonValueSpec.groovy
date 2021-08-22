package au.com.dius.pact.core.support.json

import spock.lang.Issue
import spock.lang.Specification

class JsonValueSpec extends Specification {
  @Issue('#1416')
  def 'serialise with special chars in keys'() {
    expect:
    JsonParser.parseString('{"ä": "abc"}').serialise() == '{"ä":"abc"}'
  }
}
