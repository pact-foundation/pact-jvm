package au.com.dius.pact.provider

import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class VerifierResultSpec extends Specification {

  @Unroll
  def 'merging results test'() {
    expect:
    result1.merge(result2) == result3

    where:

    result1                                                   | result2                                                   | result3
    VerificationResult.Ok.INSTANCE                            | VerificationResult.Ok.INSTANCE                            | VerificationResult.Ok.INSTANCE
    VerificationResult.Ok.INSTANCE                            | new VerificationResult.Failed([[error: 'Bang']], '', [])  | new VerificationResult.Failed([[error: 'Bang']], '', [])
    new VerificationResult.Failed([[error: 'Bang']], '', [])  | VerificationResult.Ok.INSTANCE                            | new VerificationResult.Failed([[error: 'Bang']], '', [])
    new VerificationResult.Failed([[error: 'Bang']], '', [])  | new VerificationResult.Failed([[Boom: 'Splat']], '', [])  | new VerificationResult.Failed([[error: 'Bang'], [Boom: 'Splat']], '', [])
    new VerificationResult.Failed([[error: 'Bang']], 'A', []) | new VerificationResult.Failed([[Boom: 'Splat']], '', [])  | new VerificationResult.Failed([[error: 'Bang'], [Boom: 'Splat']], 'A', [])
    new VerificationResult.Failed([[error: 'Bang']], '', [])  | new VerificationResult.Failed([[Boom: 'Splat']], 'B', []) | new VerificationResult.Failed([[error: 'Bang'], [Boom: 'Splat']], 'B', [])
    new VerificationResult.Failed([[error: 'Bang']], 'A', []) | new VerificationResult.Failed([[Boom: 'Splat']], 'B', []) | new VerificationResult.Failed([[error: 'Bang'], [Boom: 'Splat']], 'A, B', [])
    new VerificationResult.Failed([[error: 'Bang']], 'A', []) | new VerificationResult.Failed([[Boom: 'Splat']], 'A', []) | new VerificationResult.Failed([[error: 'Bang'], [Boom: 'Splat']], 'A', [])
  }
}
