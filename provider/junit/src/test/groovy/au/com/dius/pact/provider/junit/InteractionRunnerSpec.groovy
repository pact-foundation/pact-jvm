package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.core.pactbroker.PactBrokerResult
import au.com.dius.pact.provider.DefaultTestResultAccumulator
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.TestResultAccumulator
import au.com.dius.pact.provider.VerificationReporter
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.junitsupport.target.TestTarget
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import junit.framework.AssertionFailedError
import org.apache.commons.lang3.tuple.Pair
import org.jetbrains.annotations.NotNull
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.TestClass
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import java.util.function.BiConsumer
import java.util.function.Supplier

@SuppressWarnings('ClosureAsLastMethodParameter')
class InteractionRunnerSpec extends Specification {

  @SuppressWarnings('PublicInstanceField')
  class InteractionRunnerTestClass {
    @TestTarget
    public final Target target = new HttpTarget(8332)
  }

  private TestClass clazz, clazz2, failingClazz
  private reporter
  private TestResultAccumulator testResultAccumulator

  static class MockTarget implements Target {
    IProviderVerifier providerVerifier = [
      reportInteractionDescription: { }
    ] as IProviderVerifier

    @Override
    void testInteraction(@NotNull String consumerName, @NotNull Interaction interaction, @NotNull PactSource source,
                         @NotNull Map<String, ?> context) {

    }

    @Override
    void addResultCallback(@NotNull BiConsumer<VerificationResult, IProviderVerifier> callback) {
    }

    @Override
    Target withStateHandler(@NotNull Pair<Class<?>, Supplier<?>> stateHandler) {
      this
    }

    @Override
    Target withStateHandlers(@NotNull Pair<Class<?>, Supplier<?>>... stateHandlers) {
      this
    }

    @Override
    void setStateHandlers(@NotNull List<? extends Pair<Class<? extends Object>,
      Supplier<? extends Object>>> stateHandlers) {

    }

    @Override
    List<Pair<Class<? extends Object>, Supplier<? extends Object>>> getStateHandlers() {
      []
    }

    @Override
    void configureVerifier(@NotNull PactSource source, @NotNull String consumerName,
                           @NotNull Interaction interaction) {

    }

    @Override
    IProviderVerifier getVerifier() {
      providerVerifier
    }

    @SuppressWarnings('GetterMethodCouldBeProperty')
    Class getRequestClass() { null }
  }

  @SuppressWarnings('PublicInstanceField')
  static class InteractionRunnerTestClass2 {
    @TestTarget
    public final Target target = new MockTarget()
  }

  static class FailingMockTarget implements Target {
    IProviderVerifier providerVerifier = [
      reportInteractionDescription: { }
    ] as IProviderVerifier

    @Override
    void testInteraction(@NotNull String consumerName, @NotNull Interaction interaction, @NotNull PactSource source,
                         @NotNull Map<String, ?> context) {
      throw new AssertionFailedError('boom')
    }

    @Override
    void addResultCallback(@NotNull BiConsumer<VerificationResult, IProviderVerifier> callback) {
    }

    @Override
    Target withStateHandler(@NotNull Pair<Class<?>, Supplier<?>> stateHandler) {
      this
    }

    @Override
    Target withStateHandlers(@NotNull Pair<Class<?>, Supplier<?>>... stateHandlers) {
      this
    }

    @Override
    void setStateHandlers(@NotNull List<? extends Pair<Class<? extends Object>,
      Supplier<? extends Object>>> stateHandlers) {

    }

    @Override
    List<Pair<Class<? extends Object>, Supplier<? extends Object>>> getStateHandlers() {
      []
    }

    @Override
    void configureVerifier(@NotNull PactSource source, @NotNull String consumerName,
                           @NotNull Interaction interaction) {

    }

    @Override
    IProviderVerifier getVerifier() {
      providerVerifier
    }

    @SuppressWarnings('GetterMethodCouldBeProperty')
    Class getRequestClass() { null }
  }

  @SuppressWarnings('PublicInstanceField')
  static class InteractionRunnerTestClass3 {
    @TestTarget
    public final Target target = new FailingMockTarget()
  }

  def setup() {
    clazz = new TestClass(InteractionRunnerTestClass)
    clazz2 = new TestClass(InteractionRunnerTestClass2)
    failingClazz = new TestClass(InteractionRunnerTestClass3)
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
    2 * testResultAccumulator.updateTestResult(pact, _, _, _, _)
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
      publishingResultsDisabled(_) >> false
    }
    def runner = new InteractionRunner(clazz, filteredPact, UnknownPactSource.INSTANCE)

    when:
    runner.run(notifier)

    then:
    0 * testResultAccumulator.verificationReporter.reportResults(_, _, _, _)
  }

  @RestoreSystemProperties
  @SuppressWarnings('ClosureAsLastMethodParameter')
  def 'If interaction is excluded via properties than it should be marked as ignored'() {
    given:
    System.properties.setProperty('pact.filter.description', 'interaction1')
    def interaction1 = new RequestResponseInteraction('interaction1', [], new Request(), new Response())
    def interaction2 = new RequestResponseInteraction('interaction2', [], new Request(), new Response())
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])
    def notifier = Mock(RunNotifier)
    def testResultAccumulator = DefaultTestResultAccumulator.INSTANCE
    testResultAccumulator.verificationReporter = Mock(VerificationReporter) {
      publishingResultsDisabled(_) >> false
    }
    def runner = new InteractionRunner(clazz, pact, UnknownPactSource.INSTANCE)

    when:
    runner.run(notifier)

    then:
    1 * notifier.fireTestStarted({ it.displayName.startsWith('consumer - Upon interaction1') })
    0 * notifier.fireTestStarted({ it.displayName.startsWith('consumer - Upon interaction2') })
    0 * notifier.fireTestIgnored({ it.displayName.startsWith('consumer - Upon interaction1') })
    1 * notifier.fireTestIgnored({ it.displayName.startsWith('consumer - Upon interaction2') })
  }

  def 'if the test result is a success, call the notifier that the test is finished'() {
    given:
    def interaction1 = new RequestResponseInteraction('Interaction 1')
    def interaction2 = new RequestResponseInteraction('Interaction 2')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])

    def runner = new InteractionRunner(clazz2, pact, UnknownPactSource.INSTANCE)
    runner.testResultAccumulator = testResultAccumulator

    def notifier = Mock(RunNotifier)

    when:
    runner.run(notifier)

    then:
    1 * notifier.fireTestStarted({ it.displayName.startsWith('consumer - Upon Interaction 1') })

    then:
    1 * testResultAccumulator.updateTestResult(pact, interaction1, _, _, _) >> new Ok(true)

    then:
    1 * notifier.fireTestFinished({ it.displayName.startsWith('consumer - Upon Interaction 1') })

    then:
    1 * notifier.fireTestStarted({ it.displayName.startsWith('consumer - Upon Interaction 2') })

    then:
    1 * testResultAccumulator.updateTestResult(pact, interaction2, _, _, _) >> new Ok(true)

    then:
    1 * notifier.fireTestFinished({ it.displayName.startsWith('consumer - Upon Interaction 2') })
  }

  def 'if the test result is a success but pending, do not call the notifier that the test is finished'() {
    given:
    def result = new PactBrokerResult('', '', '', [], [], true, null, false, false)
    def source = new BrokerUrlSource('', '', [:], [:], null, result)
    def interaction1 = new RequestResponseInteraction('Interaction 1')
    def interaction2 = new RequestResponseInteraction('Interaction 2')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ], [:], source)

    def runner = new InteractionRunner(clazz2, pact, source)
    runner.testResultAccumulator = testResultAccumulator

    def notifier = Mock(RunNotifier)

    when:
    runner.run(notifier)

    then:
    1 * notifier.fireTestIgnored({ it.displayName.startsWith(' - Upon Interaction 1 <PENDING>') })

    then:
    1 * testResultAccumulator.updateTestResult(pact, interaction1, _, _, _) >> new Ok(true)

    then:
    1 * notifier.fireTestIgnored({ it.displayName.startsWith(' - Upon Interaction 2 <PENDING>') })

    then:
    1 * testResultAccumulator.updateTestResult(pact, interaction2, _, _, _) >> new Ok(true)
  }

  def 'if the test result is a failure, call the notifier that the test has failed'() {
    given:
    def interaction1 = new RequestResponseInteraction('Interaction 1')
    def interaction2 = new RequestResponseInteraction('Interaction 2')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])

    def runner = new InteractionRunner(failingClazz, pact, UnknownPactSource.INSTANCE)
    runner.testResultAccumulator = testResultAccumulator

    def notifier = Mock(RunNotifier)

    when:
    runner.run(notifier)

    then:
    1 * notifier.fireTestStarted({ it.displayName.startsWith('consumer - Upon Interaction 1') })

    then:
    1 * testResultAccumulator.updateTestResult(pact, interaction1, _, _, _) >> new Ok(true)

    then:
    1 * notifier.fireTestFinished({ it.displayName.startsWith('consumer - Upon Interaction 1') })

    then:
    1 * notifier.fireTestStarted({ it.displayName.startsWith('consumer - Upon Interaction 2') })

    then:
    1 * testResultAccumulator.updateTestResult(pact, interaction2, _, _, _) >> new Ok(true)

    then:
    1 * notifier.fireTestFinished({ it.displayName.startsWith('consumer - Upon Interaction 2') })
  }

  def 'if the test result is a failure but pending, do not call the notifier that the test has failed'() {
    given:
    def result = new PactBrokerResult('', '', '', [], [], true, null, false, false)
    def source = new BrokerUrlSource('', '', [:], [:], null, result)
    def interaction1 = new RequestResponseInteraction('Interaction 1')
    def interaction2 = new RequestResponseInteraction('Interaction 2')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ], [:], source)

    def runner = new InteractionRunner(failingClazz, pact, source)
    runner.testResultAccumulator = testResultAccumulator

    def notifier = Mock(RunNotifier)

    when:
    runner.run(notifier)

    then:
    1 * notifier.fireTestIgnored({ it.displayName.startsWith(' - Upon Interaction 1 <PENDING>') })

    then:
    1 * testResultAccumulator.updateTestResult(pact, interaction1, _, _, _) >> new Ok(true)

    then:
    1 * notifier.fireTestIgnored({ it.displayName.startsWith(' - Upon Interaction 2 <PENDING>') })

    then:
    1 * testResultAccumulator.updateTestResult(pact, interaction2, _, _, _) >> new Ok(true)
  }

  def 'if publishing a verification result fails, set the test result to a failure'() {
    given:
    def interaction1 = new RequestResponseInteraction('Interaction 1')
    def interaction2 = new RequestResponseInteraction('Interaction 2')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])

    def runner = new InteractionRunner(clazz, pact, UnknownPactSource.INSTANCE)
    runner.testResultAccumulator = testResultAccumulator

    def notifier = Mock(RunNotifier)

    when:
    runner.run(notifier)

    then:
    1 * notifier.fireTestStarted({ it.displayName.startsWith('consumer - Upon Interaction 1') })

    then:
    1 * testResultAccumulator.updateTestResult(pact, interaction1, _, _, _) >> new Err(['Publish failed'])

    then:
    1 * notifier.fireTestFailure({ it.description.displayName.startsWith('consumer - Upon Interaction 1') })
    1 * notifier.fireTestFinished({ it.displayName.startsWith('consumer - Upon Interaction 1') })

    then:
    1 * notifier.fireTestStarted({ it.displayName.startsWith('consumer - Upon Interaction 2') })

    then:
    1 * testResultAccumulator.updateTestResult(pact, interaction2, _, _, _) >> new Ok(true)

    then:
    1 * notifier.fireTestFinished({ it.displayName.startsWith('consumer - Upon Interaction 2') })
  }

  @RestoreSystemProperties
  def 'if the test is ignored, do not call anything'() {
    given:
    System.setProperty('pact.filter.description', 'XXXXX')
    def interaction1 = new RequestResponseInteraction('Interaction 1')
    def interaction2 = new RequestResponseInteraction('Interaction 2')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])

    def runner = new InteractionRunner(clazz, pact, UnknownPactSource.INSTANCE)
    runner.testResultAccumulator = testResultAccumulator

    def notifier = Mock(RunNotifier)

    when:
    runner.run(notifier)

    then:
    1 * notifier.fireTestIgnored({ it.displayName.startsWith('consumer - Upon Interaction 1 ') })
    1 * notifier.fireTestIgnored({ it.displayName.startsWith('consumer - Upon Interaction 2 ') })
    0 * _
  }
}
