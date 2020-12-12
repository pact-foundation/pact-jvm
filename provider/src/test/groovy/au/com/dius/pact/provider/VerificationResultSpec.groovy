package au.com.dius.pact.provider

import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.StatusMismatch
import au.com.dius.pact.core.pactbroker.TestResult
import com.github.michaelbull.result.Err
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
    new VerificationResult.Ok([])       | new VerificationResult.Ok([])         | new VerificationResult.Ok([])
    new VerificationResult.Ok([])       | failed([[error: 'Bang']], '')         | failed([[error: 'Bang']], '')
    failed([[error: 'Bang']], '')       | new VerificationResult.Ok([])         | failed([[error: 'Bang']], '')
    failed([[error: 'Bang']], '')       | failed([[Boom: 'Splat']], '')         | failed([[error: 'Bang'], [Boom: 'Splat']], '')
    failed([[error: 'Bang']], 'A')      | failed([[Boom: 'Splat']], '')         | failed([[error: 'Bang'], [Boom: 'Splat']], 'A')
    failed([[error: 'Bang']], '')       | failed([[Boom: 'Splat']], 'B')        | failed([[error: 'Bang'], [Boom: 'Splat']], 'B')
    failed([[error: 'Bang']], 'A')      | failed([[Boom: 'Splat']], 'B')        | failed([[error: 'Bang'], [Boom: 'Splat']], 'A, B')
    failed([[error: 'Bang']], 'A')      | failed([[Boom: 'Splat']], 'A')        | failed([[error: 'Bang'], [Boom: 'Splat']], 'A')
    failed([[error: 'Bang']], 'A')      | failed([[Boom: 'Splat']], 'A')        | failed([[error: 'Bang'], [Boom: 'Splat']], 'A')
    failed([[error: 'Bang']], '', true) | failed([[Boom: 'Splat']], 'A', true)  | failed([[error: 'Bang'], [Boom: 'Splat']], 'A', true)
    failed([[error: 'Bang']], '', true) | failed([[Boom: 'Splat']], 'A', false) | failed([[error: 'Bang'], [Boom: 'Splat']], 'A', false)
  }

  def 'convert to TestResult - Exception'() {
    given:
    def description = 'Request to provider failed with an exception'
    def failures = [
      new VerificationFailureType.ExceptionFailure('Request to provider method failed with an exception', new RuntimeException('Boom'))
    ]
    def verification = new VerificationResult.Failed([], description, '', failures, false, '1234ABCD')

    when:
    def result = verification.toTestResult()

    then:
    result instanceof TestResult.Failed
    result.results.size == 1
    result.results[0].interactionId == '1234ABCD'
    result.results[0].exception.message == 'Boom'
    result.results[0].description == 'Request to provider method failed with an exception'
  }

  def 'convert to TestResult - StateChangeFailure'() {
    given:
    def description = 'Provider state change callback failed'
    def failures = [
      new VerificationFailureType.StateChangeFailure(description, new StateChangeResult(new Err(new RuntimeException('Boom'))))
    ]
    def verification = new VerificationResult.Failed([], description, '', failures, false, '1234ABCD')

    when:
    def result = verification.toTestResult()

    then:
    result instanceof TestResult.Failed
    result.results.size == 1
    result.results[0].interactionId == '1234ABCD'
    result.results[0].exception instanceof RuntimeException
    result.results[0].exception.message == 'Boom'
    result.results[0].description == 'Provider state change callback failed'
  }

  def 'convert to TestResult - MismatchFailure'() {
    given:
    def diff = [
      '    {',
      '-        "doesNotExist": "Test"',
      '',
      '-        "documentId": 0',
      '+        "documentId": 0',
      '',
      '+        "documentCategoryId": 5',
      '',
      '+        "documentCategoryCode": null',
      '',
      '+        "contentLength": 0',
      '',
      '+        "tags": null',
      '+    }'].join('\n')
    def failures = [
      new VerificationFailureType.MismatchFailure(new StatusMismatch(200, 404), null, null),
      new VerificationFailureType.MismatchFailure(new HeaderMismatch('X', 'A', 'B', 'Expected a header X with value A but was B'), null, null),
      new VerificationFailureType.MismatchFailure(new BodyMismatch(null, null, 'Expected id=\'1234\' but received id=\'9905\'', '$.ns:projects.@id', diff), null, null),
    ]
    def verification = new VerificationResult.Failed([], '', '', failures, false, '1234ABCD')

    when:
    def result = verification.toTestResult()

    then:
    result instanceof TestResult.Failed
    result.results.size == 3
    result.results[0].interactionId == '1234ABCD'
    result.results[0].attribute == 'status'
    result.results[0].description == 'expected status of 200 but was 404'
    result.results[1].interactionId == '1234ABCD'
    result.results[1].attribute == 'header'
    result.results[1].description == 'Expected a header X with value A but was B'
    result.results[2].interactionId == '1234ABCD'
    result.results[2].attribute == 'body'
    result.results[2].identifier == '$.ns:projects.@id'
    result.results[2].description == 'Expected id=\'1234\' but received id=\'9905\''
    result.results[2].diff == diff
  }

  private static VerificationResult.Failed failed(List<LinkedHashMap<String, String>> details, String s, pending = false) {
    new VerificationResult.Failed(details, s, '', [], pending, null)
  }
}
