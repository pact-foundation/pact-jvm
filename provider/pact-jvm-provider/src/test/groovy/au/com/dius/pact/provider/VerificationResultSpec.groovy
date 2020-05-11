package au.com.dius.pact.provider

import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class VerificationResultSpec extends Specification {

  @Unroll
  def 'merging results test'() {
    expect:
    result1.merge(result2) == result3

    where:

    result1                             | result2                               | result3
    VerificationResult.Ok.INSTANCE      | VerificationResult.Ok.INSTANCE        | VerificationResult.Ok.INSTANCE
    VerificationResult.Ok.INSTANCE      | failed([[error: 'Bang']], '')         | failed([[error: 'Bang']], '')
    failed([[error: 'Bang']], '')       | VerificationResult.Ok.INSTANCE        | failed([[error: 'Bang']], '')
    failed([[error: 'Bang']], '')       | failed([[Boom: 'Splat']], '')         | failed([[error: 'Bang'], [Boom: 'Splat']], '')
    failed([[error: 'Bang']], 'A')      | failed([[Boom: 'Splat']], '')         | failed([[error: 'Bang'], [Boom: 'Splat']], 'A')
    failed([[error: 'Bang']], '')       | failed([[Boom: 'Splat']], 'B')        | failed([[error: 'Bang'], [Boom: 'Splat']], 'B')
    failed([[error: 'Bang']], 'A')      | failed([[Boom: 'Splat']], 'B')        | failed([[error: 'Bang'], [Boom: 'Splat']], 'A, B')
    failed([[error: 'Bang']], 'A')      | failed([[Boom: 'Splat']], 'A')        | failed([[error: 'Bang'], [Boom: 'Splat']], 'A')
    failed([[error: 'Bang']], 'A')      | failed([[Boom: 'Splat']], 'A')        | failed([[error: 'Bang'], [Boom: 'Splat']], 'A')
    failed([[error: 'Bang']], '', true) | failed([[Boom: 'Splat']], 'A', true)  | failed([[error: 'Bang'], [Boom: 'Splat']], 'A', true)
    failed([[error: 'Bang']], '', true) | failed([[Boom: 'Splat']], 'A', false) | failed([[error: 'Bang'], [Boom: 'Splat']], 'A', false)
  }

  private static VerificationResult.Failed failed(List<LinkedHashMap<String, String>> details, String s, pending = false) {
    new VerificationResult.Failed(details, s, '', [], pending)
  }
}
