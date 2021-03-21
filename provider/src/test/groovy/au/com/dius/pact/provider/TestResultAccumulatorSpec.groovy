package au.com.dius.pact.provider

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.core.model.UrlSource
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.pactbroker.TestResult
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import org.apache.commons.lang3.builder.HashCodeBuilder
import spock.lang.Issue
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
    def mockValueResolver = Mock(ValueResolver)

    when:
    testResultAccumulator.updateTestResult(mutablePact, interaction1, new TestResult.Ok(), null, mockValueResolver)
    testResultAccumulator.updateTestResult(mutablePact, interaction2, new TestResult.Ok(), null, mockValueResolver)
    testResultAccumulator.updateTestResult(mutablePact2, interaction, new TestResult.Failed(), null, mockValueResolver)
    mutablePact.interactions.first().request.matchingRules.rulesForCategory('body')
    testResultAccumulator.updateTestResult(mutablePact, interaction3, new TestResult.Ok(), null, mockValueResolver)

    then:
    1 * mockVerificationReporter.reportResults(_, new TestResult.Ok(), _, null, [])

    cleanup:
    testResultAccumulator.verificationReporter = DefaultVerificationReporter.INSTANCE
  }

  def 'updateTestResult - skip publishing verification results if publishing is disabled'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [interaction1])
    testResultAccumulator.testResults.clear()
    def reporter = testResultAccumulator.verificationReporter
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled(_) >> true
    }
    def mockValueResolver = Mock(ValueResolver)

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, new TestResult.Ok(), UnknownPactSource.INSTANCE,
      mockValueResolver)

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
      publishingResultsDisabled(_) >> false
    }
    def mockValueResolver = Mock(ValueResolver)

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, result, null, mockValueResolver)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, result, _, _, _)

    cleanup:
    testResultAccumulator.verificationReporter = reporter

    where:

    result << [new TestResult.Ok(), new TestResult.Failed()]
  }

  @Unroll
  def 'updateTestResult - publish verification results should be an "or" of all the test results'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'),
      [interaction1, interaction2])
    testResultAccumulator.testResults.clear()
    def reporter = testResultAccumulator.verificationReporter
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled(_) >> false
    }
    def mockValueResolver = Mock(ValueResolver)

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, interaction1Result, null, mockValueResolver)
    testResultAccumulator.updateTestResult(pact, interaction2, interaction2Result, null, mockValueResolver)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, result, _, _, _)

    cleanup:
    testResultAccumulator.verificationReporter = reporter

    where:

    interaction1Result      | interaction2Result      | result
    new TestResult.Ok()     | new TestResult.Ok()     | new TestResult.Ok()
    new TestResult.Ok()     | new TestResult.Failed() | new TestResult.Failed()
    new TestResult.Failed() | new TestResult.Ok()     | new TestResult.Failed()
    new TestResult.Failed() | new TestResult.Failed() | new TestResult.Failed()
  }

  def 'updateTestResult - merge the test result with any existing result'() {
    given:
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'),
      [interaction1, interaction2])
    testResultAccumulator.testResults.clear()
    def reporter = testResultAccumulator.verificationReporter
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled(_) >> false
    }
    def failedResult = new TestResult.Failed()
    def mockValueResolver = Mock(ValueResolver)

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, failedResult, null, mockValueResolver)
    testResultAccumulator.updateTestResult(pact, interaction1, new TestResult.Ok(), null, mockValueResolver)
    testResultAccumulator.updateTestResult(pact, interaction2, new TestResult.Ok(), null, mockValueResolver)

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
      publishingResultsDisabled(_) >> false
    }
    def mockValueResolver = Mock(ValueResolver)

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, new TestResult.Ok(), null, mockValueResolver)
    testResultAccumulator.updateTestResult(pact, interaction2, new TestResult.Ok(), null, mockValueResolver)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, new TestResult.Ok(), _, _, _)
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
      publishingResultsDisabled(_) >> false
    }
    System.setProperty('pact.provider.tag', 'updateTestResultTag')
    def mockValueResolver = SystemPropertyResolver.INSTANCE

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, new TestResult.Ok(), null, mockValueResolver)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, new TestResult.Ok(), _, _,
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
      publishingResultsDisabled(_) >> false
    }
    System.setProperty('pact.provider.tag', 'tag1,tag2 , tag3 ')
    def mockValueResolver = SystemPropertyResolver.INSTANCE

    when:
    testResultAccumulator.updateTestResult(pact, interaction1, new TestResult.Ok(), null, mockValueResolver)

    then:
    1 * testResultAccumulator.verificationReporter.reportResults(_, new TestResult.Ok(), _, _,
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
    def builder = new HashCodeBuilder(91, 47)
    args.each { builder.append(it) }
    builder.toHashCode()
  }

  @Issue('#1266')
  @SuppressWarnings(['AbcMetric', 'VariableName', 'MethodSize', 'UnnecessaryObjectReferences', 'UnnecessaryGetter'])
  def 'updateTestResult - with a pending and non-pending pact'() {
    given:

    def provider1 = new Provider('provider1')

    def consumer1 = new Consumer('consumer1')
    def consumer2 = new Consumer('consumer2')
    def consumer3 = new Consumer('consumer3')

    def body = OptionalBody.missing()
    def mr = new MatchingRulesImpl()
    def g = new Generators()

    def interaction1_1 = new Message('interaction1_1', [], body, mr, g, [:], 'interaction1_1')
    def interaction1_2 = new Message('interaction1_2', [], body, mr, g, [:], 'interaction1_2')
    def interaction1_3 = new Message('interaction1_3', [], body, mr, g, [:], 'interaction1_3')
    def interaction1_4 = new Message('interaction1_4', [], body, mr, g, [:], 'interaction1_4')
    def interaction1_5 = new Message('interaction1_5', [], body, mr, g, [:], 'interaction1_5')

    def interaction2_1 = new Message('interaction2_1', [], body, mr, g, [:], 'interaction2_1')
    def interaction2_2 = new Message('interaction2_2', [], body, mr, g, [:], 'interaction2_2')
    def interaction2_3 = new Message('interaction2_3', [], body, mr, g, [:], 'interaction2_3')
    def interaction2_4 = new Message('interaction2_4', [], body, mr, g, [:], 'interaction2_4')

    def interaction3_1 = new Message('interaction3_1', [], body, mr, g, [:], 'interaction3_1')
    def interaction3_2 = new Message('interaction3_2', [], body, mr, g, [:], 'interaction3_2')
    def interaction3_3 = new Message('interaction3_3', [], body, mr, g, [:], 'interaction3_3')
    def interaction3_4 = new Message('interaction3_4', [], body, mr, g, [:], 'interaction3_4')

    def pact1 = new MessagePact(provider1, consumer1, [interaction1_1, interaction1_2, interaction1_3, interaction1_4,
                                                       interaction1_5])
    def source1 = new BrokerUrlSource('http://url1', 'http://broker', [:], [:], 'master')
    def pact2 = new MessagePact(provider1, consumer2, [interaction2_1, interaction2_2, interaction2_3, interaction2_4])
    def source2 = new BrokerUrlSource('http://url2', 'http://broker', [:], [:], 'master')
    def pact3 = new MessagePact(provider1, consumer2, [interaction2_1, interaction2_2, interaction2_3, interaction2_4])
    def source3 = new BrokerUrlSource('http://url3', 'http://broker', [:], [:], 'tag1')
    def pact4 = new MessagePact(provider1, consumer3, [interaction3_1, interaction3_2, interaction3_3, interaction3_4])
    def source4 = new BrokerUrlSource('http://url4', 'http://broker', [:], [:], 'master')

    testResultAccumulator.testResults.clear()
    def reporter = testResultAccumulator.verificationReporter
    def verificationReporter = Mock(VerificationReporter)
    testResultAccumulator.verificationReporter = verificationReporter
    def mockValueResolver = Mock(ValueResolver)
    def exception = new RuntimeException()

    when:
    testResultAccumulator.updateTestResult(pact1, interaction1_1,
      [new VerificationResult.Ok(new HashSet(['interaction1_1']))], source1, mockValueResolver)
    testResultAccumulator.updateTestResult(pact1, interaction1_3,
      [new VerificationResult.Ok(new HashSet(['interaction1_3']))], source1, mockValueResolver)
    testResultAccumulator.updateTestResult(pact2, interaction2_1,
      [new VerificationResult.Ok(new HashSet(['interaction2_1']))], source2, mockValueResolver)
    testResultAccumulator.updateTestResult(pact3, interaction2_1,
      [new VerificationResult.Ok(new HashSet(['interaction2_1']))], source3, mockValueResolver)
    testResultAccumulator.updateTestResult(pact1, interaction1_2,
      [new VerificationResult.Ok(new HashSet(['interaction1_2']))], source1, mockValueResolver)
    testResultAccumulator.updateTestResult(pact2, interaction2_4,
      [new VerificationResult.Ok(new HashSet(['interaction2_4']))], source2, mockValueResolver)
    testResultAccumulator.updateTestResult(pact3, interaction2_4,
      [new VerificationResult.Failed('failed', 'failed',
        [
          interaction2_4: [new VerificationFailureType.ExceptionFailure('failed', exception)]
        ], true)
      ], source3, mockValueResolver)
    testResultAccumulator.updateTestResult(pact2, interaction2_2,
      [new VerificationResult.Ok(new HashSet(['interaction2_2']))], source2, mockValueResolver)
    testResultAccumulator.updateTestResult(pact3, interaction2_2,
      [new VerificationResult.Ok(new HashSet(['interaction2_2']))], source3, mockValueResolver)
    testResultAccumulator.updateTestResult(pact2, interaction2_3,
      [new VerificationResult.Ok(new HashSet(['interaction2_3']))], source2, mockValueResolver)
    testResultAccumulator.updateTestResult(pact3, interaction2_3,
      [new VerificationResult.Ok(new HashSet(['interaction2_3']))], source3, mockValueResolver)
    testResultAccumulator.updateTestResult(pact1, interaction1_4,
      [new VerificationResult.Ok(new HashSet(['interaction1_4']))], source1, mockValueResolver)
    testResultAccumulator.updateTestResult(pact4, interaction3_1,
      [new VerificationResult.Ok(new HashSet(['interaction3_1']))], source4, mockValueResolver)
    testResultAccumulator.updateTestResult(pact4, interaction3_3,
      [new VerificationResult.Ok(new HashSet(['interaction3_3']))], source4, mockValueResolver)
    testResultAccumulator.updateTestResult(pact4, interaction3_2,
      [new VerificationResult.Ok(new HashSet(['interaction3_2']))], source4, mockValueResolver)
    testResultAccumulator.updateTestResult(pact4, interaction3_4,
      [new VerificationResult.Ok(new HashSet(['interaction3_4']))], source4, mockValueResolver)
    testResultAccumulator.updateTestResult(pact1, interaction1_5,
      [new VerificationResult.Ok(new HashSet(['interaction1_5']))], source1, mockValueResolver)

    then:
    verificationReporter.publishingResultsDisabled(_) >> false
    1 * verificationReporter.reportResults(pact2,
      new TestResult.Ok(['interaction2_3', 'interaction2_1', 'interaction2_4', 'interaction2_2'] as HashSet), _, _,
      [])
    1 * verificationReporter.reportResults(pact3,
      new TestResult.Failed(
        [
          [exception: exception, description: 'failed', interactionId: 'interaction2_4'],
          [interactionId: 'interaction2_1'],
          [interactionId: 'interaction2_2'],
          [interactionId: 'interaction2_3']
        ], 'failed'), _, _,
      [])
    1 * verificationReporter.reportResults(pact4,
      new TestResult.Ok(['interaction3_1', 'interaction3_2', 'interaction3_3', 'interaction3_4'] as HashSet), _, _,
      [])
    1 * verificationReporter.reportResults(pact1,
      new TestResult.Ok(
        ['interaction1_1', 'interaction1_2', 'interaction1_3', 'interaction1_4', 'interaction1_5'] as HashSet
      ), _, _, [])
    0 * verificationReporter._
    testResultAccumulator.testResults.isEmpty()

    cleanup:
    testResultAccumulator.verificationReporter = reporter
  }
}
