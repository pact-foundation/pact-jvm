package au.com.dius.pact.provider

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.core.model.UrlSource
import au.com.dius.pact.core.pactbroker.TestResult
import org.apache.commons.lang3.builder.HashCodeBuilder
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

  @RestoreSystemProperties
  def 'lookupProviderVersion - trims snapshot if system property is set'() {
    given:
    System.setProperty('pact.provider.version', '1.2.3-SNAPSHOT')
    System.setProperty('pact.provider.version.trimSnapshot', 'true')

    expect:
    testResultAccumulator.lookupProviderVersion() == '1.2.3'
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
    testResultAccumulator.updateTestResult(mutablePact, interaction1, new TestResult.Ok(null), null)
    testResultAccumulator.updateTestResult(mutablePact, interaction2, new TestResult.Ok(null), null)
    testResultAccumulator.updateTestResult(mutablePact2, interaction, new TestResult.Failed(), null)
    mutablePact.interactions.first().request.matchingRules.rulesForCategory('body')
    testResultAccumulator.updateTestResult(mutablePact, interaction3, new TestResult.Ok(null), null)

    then:
    1 * mockVerificationReporter.reportResults(_, new TestResult.Ok(null), _, null, [])

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
    testResultAccumulator.updateTestResult(pact, interaction1, new TestResult.Ok(null), UnknownPactSource.INSTANCE)

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
    testResultAccumulator.updateTestResult(pact, interaction1, result, null)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, result, _, _, _)

    cleanup:
    testResultAccumulator.verificationReporter = reporter

    where:

    result << [new TestResult.Ok(null), new TestResult.Failed()]
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
    testResultAccumulator.updateTestResult(pact, interaction1, interaction1Result, null)
    testResultAccumulator.updateTestResult(pact, interaction2, interaction2Result, null)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, result, _, _, _)

    cleanup:
    testResultAccumulator.verificationReporter = reporter

    where:

    interaction1Result      | interaction2Result      | result
    new TestResult.Ok(null) | new TestResult.Ok(null) | new TestResult.Ok(null)
    new TestResult.Ok(null) | new TestResult.Failed() | new TestResult.Failed()
    new TestResult.Failed() | new TestResult.Ok(null) | new TestResult.Failed()
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
    testResultAccumulator.updateTestResult(pact, interaction1, failedResult, null)
    testResultAccumulator.updateTestResult(pact, interaction1, new TestResult.Ok(null), null)
    testResultAccumulator.updateTestResult(pact, interaction2, new TestResult.Ok(null), null)

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
    testResultAccumulator.updateTestResult(pact, interaction1, new TestResult.Ok(null), null)
    testResultAccumulator.updateTestResult(pact, interaction2, new TestResult.Ok(null), null)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, new TestResult.Ok(null), _, _, _)
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
    testResultAccumulator.updateTestResult(pact, interaction1, new TestResult.Ok(null), null)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, new TestResult.Ok(null), _, _,
      ['updateTestResultTag'])
    testResultAccumulator.testResults.isEmpty()

    cleanup:
    testResultAccumulator.verificationReporter = reporter
  }

  @RestoreSystemProperties
  @SuppressWarnings('UnnecessaryGetter')
  def 'updateTestResult - include all the provider tags'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'),
      [interaction1])
    testResultAccumulator.testResults.clear()
    def reporter = testResultAccumulator.verificationReporter
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled() >> false
    }
    System.setProperty('pact.provider.tag', 'tag1,tag2 , tag3 ')

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, new TestResult.Ok(null), null)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, new TestResult.Ok(null), _, _,
      ['tag1', 'tag2', 'tag3'])
    testResultAccumulator.testResults.isEmpty()

    cleanup:
    testResultAccumulator.verificationReporter = reporter
  }

  @Unroll
  @SuppressWarnings('LineLength')
  def 'calculatePactHash includes the tag if one is available'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'),
      [interaction1])

    when:
    def hash = testResultAccumulator.calculatePactHash(pact, source)

    then:
    hash == calculatedHash

    where:

    source                                                                                                    | calculatedHash
    null                                                                                                      | calculateHash('consumer', 'provider')
    new UrlSource('http://pact.io')                                                                           | calculateHash('consumer', 'provider')
    new FileSource('/tmp/pact' as File)                                                                       | calculateHash('consumer', 'provider')
    new FileSource('/tmp/pact' as File)                                                                       | calculateHash('consumer', 'provider')
    new BrokerUrlSource('https://test.pact.dius.com.au', 'https://test.pact.dius.com.au', [:], [:])           | calculateHash('consumer', 'provider')
    new BrokerUrlSource('https://test.pact.dius.com.au', 'https://test.pact.dius.com.au', [:], [:], 'master') | calculateHash('consumer', 'provider', 'master')
  }

  private int calculateHash(String... args) {
    def builder = new HashCodeBuilder()
    args.each { builder.append(it) }
    builder.toHashCode()
  }
}
