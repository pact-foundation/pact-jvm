package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.model.Request
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.model.Response
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.provider.DefaultTestResultAccumulator
import au.com.dius.pact.provider.TestResultAccumulator
import au.com.dius.pact.provider.VerificationReporter
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.junitsupport.target.TestTarget
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.TestClass
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

class InteractionRunnerSpec extends Specification {

  @SuppressWarnings('PublicInstanceField')
  class InteractionRunnerTestClass {
    @TestTarget
    public final Target target = new HttpTarget(8332)
  }

  private clazz
  private reporter
  private TestResultAccumulator testResultAccumulator

  def setup() {
    clazz = new TestClass(InteractionRunnerTestClass)
    reporter = Mock(VerificationReporter)
    testResultAccumulator = Mock(TestResultAccumulator)
  }

  def 'publish a failed verification result if any before step fails'() {
    given:
    def interaction1 = new RequestResponseInteraction('Interaction 1',
            [ new ProviderState('Test State') ], new Request(), new Response())
    def interaction2 = new RequestResponseInteraction('Interaction 2', [], new Request(), new Response())
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])

    def runner = new InteractionRunner(clazz, pact, UnknownPactSource.INSTANCE)
    runner.testResultAccumulator = testResultAccumulator

    when:
    runner.run([:] as RunNotifier)

    then:
    2 * testResultAccumulator.updateTestResult(pact, _, _)
  }

  @RestoreSystemProperties
  def 'provider version trims -SNAPSHOT'() {
    given:
    System.setProperty('pact.provider.version', '1.0.0-SNAPSHOT-wn23jhd')
    def interaction1 = new RequestResponseInteraction('Interaction 1', [], new Request(), new Response())
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1 ])

    def filteredPact = new FilteredPact(pact, { it.description == 'Interaction 1' })
    def runner = new InteractionRunner(clazz, filteredPact, UnknownPactSource.INSTANCE)

    // Property true
    when:
    System.setProperty('pact.provider.version.trimSnapshot', 'true')
    def providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-wn23jhd'

    // Property false
    when:
    System.setProperty('pact.provider.version.trimSnapshot', 'false')
    providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-SNAPSHOT-wn23jhd'

    // Property unexpected value
    when:
    System.setProperty('pact.provider.version.trimSnapshot', 'erwf')
    providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-SNAPSHOT-wn23jhd'

    // Property not present
    when:
    System.clearProperty('pact.provider.version.trimSnapshot')
    providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-SNAPSHOT-wn23jhd'
  }

  @RestoreSystemProperties
  def 'updateTestResult - if FilteredPact and not all interactions verified then no call on verificationReporter'() {
    given:
    def interaction1 = new RequestResponseInteraction('interaction1', [], new Request(), new Response())
    def interaction2 = new RequestResponseInteraction('interaction2', [], new Request(), new Response())
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])
    def notifier = Mock(RunNotifier)
    def filteredPact = new FilteredPact(pact, { it.description == 'interaction1' })
    def testResultAccumulator = DefaultTestResultAccumulator.INSTANCE
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled() >> false
    }
    def runner = new InteractionRunner(clazz, filteredPact, UnknownPactSource.INSTANCE)

    when:
    runner.run(notifier)

    then:
    0 * testResultAccumulator.verificationReporter.reportResults(_, _, _, _)
  }
}
