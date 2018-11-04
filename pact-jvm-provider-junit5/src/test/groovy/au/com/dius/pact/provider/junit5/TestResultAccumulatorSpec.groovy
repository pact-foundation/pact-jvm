package au.com.dius.pact.provider.junit5

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.RequestResponsePact
import au.com.dius.pact.provider.DefaultVerificationReporter
import au.com.dius.pact.provider.VerificationReporter
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

class TestResultAccumulatorSpec extends Specification {

  static interaction1 = new RequestResponseInteraction('interaction1', [], new Request())
  static interaction2 = new RequestResponseInteraction('interaction2', [], new Request())
  static pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [
    interaction1, interaction2
  ])
  static interaction1Hash = TestResultAccumulator.INSTANCE.calculateInteractionHash(interaction1)
  static interaction2Hash = TestResultAccumulator.INSTANCE.calculateInteractionHash(interaction2)

  @RestoreSystemProperties
  def 'lookupProviderVersion - returns the version set in the system properties'() {
    given:
    System.setProperty('pact.provider.version', '1.2.3')

    expect:
    TestResultAccumulator.INSTANCE.lookupProviderVersion() == '1.2.3'
  }

  def 'lookupProviderVersion - returns a default value if there is no version set in the system properties'() {
    expect:
    TestResultAccumulator.INSTANCE.lookupProviderVersion() == '0.0.0'
  }

  @Unroll
  @SuppressWarnings('LineLength')
  def 'allInteractionsVerified returns #result when #condition'() {
    expect:
    TestResultAccumulator.INSTANCE.allInteractionsVerified(pact, results) == result

    where:

    condition                                           | results                                                | result
    'no results have been received'                     | [:]                                                    | false
    'only some results have been received'              | [(interaction1Hash): true]                             | false
    'all results have been received'                    | [(interaction1Hash): true, (interaction2Hash): true]   | true
    'all results have been received but some are false' | [(interaction1Hash): true, (interaction2Hash): false]  | false
    'all results have been received but all are false'  | [(interaction1Hash): false, (interaction2Hash): false] | false
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
    TestResultAccumulator.INSTANCE.verificationReporter = mockVerificationReporter

    when:
    TestResultAccumulator.INSTANCE.updateTestResult(mutablePact, interaction1, true)
    TestResultAccumulator.INSTANCE.updateTestResult(mutablePact, interaction2, true)
    TestResultAccumulator.INSTANCE.updateTestResult(mutablePact2, interaction, false)
    mutablePact.interactions.first().request.matchingRules.rulesForCategory('body')
    TestResultAccumulator.INSTANCE.updateTestResult(mutablePact, interaction3, true)

    then:
    1 * mockVerificationReporter.reportResults(_, true, _, null)

    cleanup:
    TestResultAccumulator.INSTANCE.verificationReporter = DefaultVerificationReporter.INSTANCE
  }

  def 'updateTestResult - skip publishing verification results if publishing is disabled'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [interaction1])
    def reporter = TestResultAccumulator.INSTANCE.verificationReporter
    TestResultAccumulator.INSTANCE.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled() >> true
    }

    when:
    TestResultAccumulator.INSTANCE.updateTestResult(pact, interaction1, true)

    then:
    0 * TestResultAccumulator.INSTANCE.verificationReporter.reportResults(_, _, _, _)

    cleanup:
    TestResultAccumulator.INSTANCE.verificationReporter = reporter
  }

  def 'updateTestResult - publish verification results if publishing is enabled'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [interaction1])
    def reporter = TestResultAccumulator.INSTANCE.verificationReporter
    TestResultAccumulator.INSTANCE.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled() >> false
    }

    when:
    TestResultAccumulator.INSTANCE.updateTestResult(pact, interaction1, true)

    then:
    1 * TestResultAccumulator.INSTANCE.verificationReporter.reportResults(pact, true, _, null)

    cleanup:
    TestResultAccumulator.INSTANCE.verificationReporter = reporter
  }

}
