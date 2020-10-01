package au.com.dius.pact.core.pactbroker

import spock.lang.Specification
import spock.lang.Unroll

class ConsumerVersionSelectorSpec extends Specification {

  @Unroll
  def 'convert to JSON'() {
    expect:
    new ConsumerVersionSelector(tag, latest, consumer).toJson().serialise() == json

    where:

    tag  | latest | consumer | json
    'A'  | true   | null     | '{"latest":true,"tag":"A"}'
    'A'  | false  | null     | '{"latest":false,"tag":"A"}'
    'A'  | false  | 'Bob'    | '{"consumer":"Bob","latest":false,"tag":"A"}'
    null | false  | 'Bob'    | '{"consumer":"Bob","latest":false}'
  }
}
