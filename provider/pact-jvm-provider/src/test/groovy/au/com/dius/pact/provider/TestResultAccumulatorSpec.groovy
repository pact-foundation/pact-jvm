package au.com.dius.pact.provider

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

class TestResultAccumulatorSpec extends Specification {

  static interaction1 = new RequestResponseInteraction('interaction1', [], new Request())
  static interaction2 = new RequestResponseInteraction('interaction2', [], new Request())
  static pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [
    interaction1, interaction2
  ])
  static DefaultTestResultAccumulator testResultAccumulator = DefaultTestResultAccumulator.INSTANCE
  static interaction1Hash = testResultAccumulator.calculateInteractionHash(interaction1)
  static interaction2Hash = testResultAccumulator.calculateInteractionHash(interaction2)

  @RestoreSystemProperties
  def 'lookupProviderVersion - returns the version set in the system properties'() {
    given:
    System.setProperty('pact.provider.version', '1.2.3')

    expect:
    testResultAccumulator.lookupProviderVersion() == '1.2.3'
  }

  def 'lookupProviderVersion - returns a default value if there is no version set in the system properties'() {
    expect:
    testResultAccumulator.lookupProviderVersion() == '0.0.0'
  }

  @Unroll
  @SuppressWarnings('LineLength')
  def 'allInteractionsVerified returns #result when #condition'() {
    expect:
    testResultAccumulator.allInteractionsVerified(pact, results) == result

    where:

    condition                                           | results                                                | result
    'no results have been received'                     | [:]                                                    | false
    'only some results have been received'              | [(interaction1Hash): true]                             | false
    'all results have been received'                    | [(interaction1Hash): true, (interaction2Hash): true]   | true
    'all results have been received but some are false' | [(interaction1Hash): true, (interaction2Hash): false]  | true
    'all results have been received but all are false'  | [(interaction1Hash): false, (interaction2Hash): false] | true
  }

  def 'accumulator should not rely on the Pact class hash codes'() {
    given:
    def interaction3 = new RequestResponseInteraction('interaction3', [], new Request())
    def mutablePact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [
      interaction1, interaction2, interaction3
    ])
    def interaction = new RequestResponseInteraction('interaction1', [], new Request())
    def mutablePact2 = new RequestResponsePact(new Provider('provider'), new Consumer('consumer2'), [
      interaction
    ])
    def mockVerificationReporter = Mock(VerificationReporter)
    testResultAccumulator.verificationReporter = mockVerificationReporter

    when:
    testResultAccumulator.updateTestResult(mutablePact, interaction1, true)
    testResultAccumulator.updateTestResult(mutablePact, interaction2, true)
    testResultAccumulator.updateTestResult(mutablePact2, interaction, false)
    mutablePact.interactions.first().request.matchingRules.rulesForCategory('body')
    testResultAccumulator.updateTestResult(mutablePact, interaction3, true)

    then:
    1 * mockVerificationReporter.reportResults(_, true, _, null)

    cleanup:
    testResultAccumulator.verificationReporter = DefaultVerificationReporter.INSTANCE
  }

  def 'updateTestResult - skip publishing verification results if publishing is disabled'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [interaction1])
    testResultAccumulator.testResults.clear()
    def reporter = testResultAccumulator.verificationReporter
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled() >> true
    }

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, true)

    then:
    0 * testResultAccumulator.verificationReporter.reportResults(_, _, _, _)

    cleanup:
    testResultAccumulator.verificationReporter = reporter
  }

  @Unroll
  def 'updateTestResult - publish #result verification results if publishing is enabled'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [interaction1])
    testResultAccumulator.testResults.clear()
    def reporter = testResultAccumulator.verificationReporter
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled() >> false
    }

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, result)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, result, _, _)

    cleanup:
    testResultAccumulator.verificationReporter = reporter

    where:

    result << [true, false]
  }

  @Unroll
  def 'updateTestResult - publish verification results should be an or of all the test results'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'),
      [interaction1, interaction2])
    testResultAccumulator.testResults.clear()
    def reporter = testResultAccumulator.verificationReporter
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled() >> false
    }

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, interaction1Result)
    testResultAccumulator.updateTestResult(pact, interaction2, interaction2Result)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, result, _, _)

    cleanup:
    testResultAccumulator.verificationReporter = reporter

    where:

    interaction1Result | interaction2Result | result
    true               | true               | true
    true               | false              | false
    false              | true               | false
    false              | false              | false
  }

}
