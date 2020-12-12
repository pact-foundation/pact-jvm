package au.com.dius.pact.core.pactbroker

import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class TestResultSpec extends Specification {

  @Unroll
  def 'merging results test'() {
    expect:
    result1.merge(result2) == result3

    where:

    result1                                                            | result2                                                            | result3
    new TestResult.Ok(null)                                            | new TestResult.Ok(null)                                            | new TestResult.Ok(null)
    new TestResult.Ok(null)                                            | new TestResult.Ok('123')                                           | new TestResult.Ok('123')
    new TestResult.Ok('123')                                           | new TestResult.Ok(null)                                            | new TestResult.Ok('123')
    new TestResult.Ok('123')                                           | new TestResult.Ok('456')                                           | new TestResult.Ok('123')
    new TestResult.Ok(null)                                            | new TestResult.Failed([[error: 'Bang']], '')                       | new TestResult.Failed([[error: 'Bang']], '')
    new TestResult.Ok('123')                                           | new TestResult.Failed([[error: 'Bang']], '')                       | new TestResult.Failed([[error: 'Bang'], [interactionId: '123']], '')
    new TestResult.Ok('123')                                           | new TestResult.Failed([[error: 'Bang', interactionId: '123']], '') | new TestResult.Failed([[error: 'Bang', interactionId: '123']], '')
    new TestResult.Failed([[error: 'Bang']], '')                       | new TestResult.Ok(null)                                            | new TestResult.Failed([[error: 'Bang']], '')
    new TestResult.Failed([[error: 'Bang']], '')                       | new TestResult.Ok('123')                                           | new TestResult.Failed([[error: 'Bang'], [interactionId: '123']], '')
    new TestResult.Failed([[error: 'Bang', interactionId: '123']], '') | new TestResult.Ok('123')                                           | new TestResult.Failed([[error: 'Bang', interactionId: '123']], '')
    new TestResult.Failed([[error: 'Bang']], '')                       | new TestResult.Failed(['Boom', 'Splat'], '')                       | new TestResult.Failed([[error: 'Bang'], 'Boom', 'Splat'], '')
    new TestResult.Failed([[error: 'Bang']], 'A')                      | new TestResult.Failed(['Boom', 'Splat'], '')                       | new TestResult.Failed([[error: 'Bang'], 'Boom', 'Splat'], 'A')
    new TestResult.Failed([[error: 'Bang']], '')                       | new TestResult.Failed(['Boom', 'Splat'], 'B')                      | new TestResult.Failed([[error: 'Bang'], 'Boom', 'Splat'], 'B')
    new TestResult.Failed([[error: 'Bang']], 'A')                      | new TestResult.Failed(['Boom', 'Splat'], 'B')                      | new TestResult.Failed([[error: 'Bang'], 'Boom', 'Splat'], 'A, B')
    new TestResult.Failed([[error: 'Bang']], 'A')                      | new TestResult.Failed(['Boom', 'Splat'], 'A')                      | new TestResult.Failed([[error: 'Bang'], 'Boom', 'Splat'], 'A')
  }
}
