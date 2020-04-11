package au.com.dius.pact.provider

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.pactbroker.TestResult
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

class TestResultAccumulatorSpec extends Specification {

  static interaction1 = new RequestResponseInteraction('interaction1', [], new Request(), new Response())
  static interaction2 = new RequestResponseInteraction('interaction2', [], new Request(), new Response())
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
    testResultAccumulator.unverifiedInteractions(pact, results).empty == result

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
    def interaction3 = new RequestResponseInteraction('interaction3', [], new Request(), new Response())
    def mutablePact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [
      interaction1, interaction2, interaction3
    ])
    def interaction = new RequestResponseInteraction('interaction1', [], new Request(), new Response())
    def mutablePact2 = new RequestResponsePact(new Provider('provider'), new Consumer('consumer2'), [
      interaction
    ])
    def mockVerificationReporter = Mock(VerificationReporter)
    testResultAccumulator.verificationReporter = mockVerificationReporter

    when:
    testResultAccumulator.updateTestResult(mutablePact, interaction1, TestResult.Ok.INSTANCE)
    testResultAccumulator.updateTestResult(mutablePact, interaction2, TestResult.Ok.INSTANCE)
    testResultAccumulator.updateTestResult(mutablePact2, interaction, new TestResult.Failed())
    mutablePact.interactions.first().request.matchingRules.rulesForCategory('body')
    testResultAccumulator.updateTestResult(mutablePact, interaction3, TestResult.Ok.INSTANCE)

    then:
    1 * mockVerificationReporter.reportResults(_, TestResult.Ok.INSTANCE, _, null, null)

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
    0 * testResultAccumulator.verificationReporter.reportResults(_, _, _, _, _)

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
    1 * testResultAccumulator.verificationReporter.reportResults(_, result, _, _, _)

    cleanup:
    testResultAccumulator.verificationReporter = reporter

    where:

    result << [TestResult.Ok.INSTANCE, new TestResult.Failed()]
  }

  @Unroll
  def 'updateTestResult - publish verification results should be an "or" of all the test results'() {
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
    1 * testResultAccumulator.verificationReporter.reportResults(_, result, _, _, _)

    cleanup:
    testResultAccumulator.verificationReporter = reporter

    where:

    interaction1Result      | interaction2Result      | result
    TestResult.Ok.INSTANCE  | TestResult.Ok.INSTANCE  | TestResult.Ok.INSTANCE
    TestResult.Ok.INSTANCE  | new TestResult.Failed() | new TestResult.Failed()
    new TestResult.Failed() | TestResult.Ok.INSTANCE  | new TestResult.Failed()
    new TestResult.Failed() | new TestResult.Failed() | new TestResult.Failed()
  }

  def 'updateTestResult - merge the test result with any existing result'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'),
      [interaction1, interaction2])
    testResultAccumulator.testResults.clear()
    def reporter = testResultAccumulator.verificationReporter
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled() >> false
    }
    def failedResult = new TestResult.Failed()

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, failedResult)
    testResultAccumulator.updateTestResult(pact, interaction1, TestResult.Ok.INSTANCE)
    testResultAccumulator.updateTestResult(pact, interaction2, TestResult.Ok.INSTANCE)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, failedResult, _, _, _)

    cleanup:
    testResultAccumulator.verificationReporter = reporter
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'updateTestResult - clear the results when they are published'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'),
      [interaction1, interaction2])
    testResultAccumulator.testResults.clear()
    def reporter = testResultAccumulator.verificationReporter
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled() >> false
    }

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, TestResult.Ok.INSTANCE)
    testResultAccumulator.updateTestResult(pact, interaction2, TestResult.Ok.INSTANCE)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, TestResult.Ok.INSTANCE, _, _, _)
    testResultAccumulator.testResults.isEmpty()

    cleanup:
    testResultAccumulator.verificationReporter = reporter
  }

  @RestoreSystemProperties
  @SuppressWarnings('UnnecessaryGetter')
  def 'updateTestResult - include the provider tag'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'),
      [interaction1])
    testResultAccumulator.testResults.clear()
    def reporter = testResultAccumulator.verificationReporter
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled() >> false
    }
    System.setProperty('pact.provider.tag', 'updateTestResultTag')

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, TestResult.Ok.INSTANCE)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, TestResult.Ok.INSTANCE, _, _,
      'updateTestResultTag')
    testResultAccumulator.testResults.isEmpty()

    cleanup:
    testResultAccumulator.verificationReporter = reporter
  }

}
