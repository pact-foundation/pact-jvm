package au.com.dius.pact.core.pactbroker

import spock.lang.Specification
import spock.lang.Unroll

class ConsumerVersionSelectorSpec extends Specification {

  @Unroll
  def 'convert to JSON'() {
    expect:
    new ConsumerVersionSelector(tag, latest, consumer, fallback).toJson().serialise() == json

    where:

    tag  | latest | consumer | fallback | json
    'A'  | true   | null     | null     | '{"latest":true,"tag":"A"}'
    'A'  | true   | null     | 'B'      | '{"fallbackTag":"B","latest":true,"tag":"A"}'
    'A'  | false  | null     | null     | '{"latest":false,"tag":"A"}'
    'A'  | false  | 'Bob'    | null     | '{"consumer":"Bob","latest":false,"tag":"A"}'
    null | false  | 'Bob'    | null     | '{"consumer":"Bob","latest":false}'
  }
}
