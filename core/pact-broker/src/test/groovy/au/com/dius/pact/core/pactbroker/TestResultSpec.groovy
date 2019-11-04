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

    result1                         | result2                                  | result3
    TestResult.Ok.INSTANCE          | TestResult.Ok.INSTANCE                   | TestResult.Ok.INSTANCE
    TestResult.Ok.INSTANCE          | new TestResult.Failed(['Bang'], '')          | new TestResult.Failed(['Bang'], '')
    new TestResult.Failed(['Bang'], '') | TestResult.Ok.INSTANCE                   | new TestResult.Failed(['Bang'], '')
    new TestResult.Failed(['Bang'], '') | new TestResult.Failed(['Boom', 'Splat'], '') | new TestResult.Failed(['Bang', 'Boom', 'Splat'], '')
    new TestResult.Failed(['Bang'], 'A') | new TestResult.Failed(['Boom', 'Splat'], '') | new TestResult.Failed(['Bang', 'Boom', 'Splat'], 'A')
    new TestResult.Failed(['Bang'], '') | new TestResult.Failed(['Boom', 'Splat'], 'B') | new TestResult.Failed(['Bang', 'Boom', 'Splat'], 'B')
    new TestResult.Failed(['Bang'], 'A') | new TestResult.Failed(['Boom', 'Splat'], 'B') | new TestResult.Failed(['Bang', 'Boom', 'Splat'], 'A, B')
    new TestResult.Failed(['Bang'], 'A') | new TestResult.Failed(['Boom', 'Splat'], 'A') | new TestResult.Failed(['Bang', 'Boom', 'Splat'], 'A')
  }

}
