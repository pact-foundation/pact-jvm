package au.com.dius.pact.pactbroker

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
    TestResult.Ok.INSTANCE          | new TestResult.Failed(['Bang'])          | new TestResult.Failed(['Bang'])
    new TestResult.Failed(['Bang']) | TestResult.Ok.INSTANCE                   | new TestResult.Failed(['Bang'])
    new TestResult.Failed(['Bang']) | new TestResult.Failed(['Boom', 'Splat']) | new TestResult.Failed(['Bang', 'Boom', 'Splat'])
  }

}
