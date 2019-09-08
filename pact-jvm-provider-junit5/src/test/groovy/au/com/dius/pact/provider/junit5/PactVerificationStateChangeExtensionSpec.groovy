package au.com.dius.pact.provider.junit5

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.RequestResponsePact
import au.com.dius.pact.pactbroker.TestResult
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.TestResultAccumulator
import au.com.dius.pact.provider.junit.MissingStateChangeMethod
import au.com.dius.pact.provider.junit.State
import au.com.dius.pact.provider.junit.StateChangeAction
import au.com.dius.pact.support.expressions.ValueResolver
import org.junit.jupiter.api.extension.ExtensionContext
import spock.lang.Specification
import spock.lang.Unroll

class PactVerificationStateChangeExtensionSpec extends Specification {

  private PactVerificationStateChangeExtension verificationExtension
  Interaction interaction
  private TestResultAccumulator testResultAcc

  static class TestClass {

    boolean stateCalled = false
    boolean state2Called = false
    boolean state2TeardownCalled = false
    def state3Called = null

    @State('Test 1')
    void state1() {
      stateCalled = true
    }

    @State(['State 2', 'Test 2'])
    void state2() {
      state2Called = true
    }

    @State(value = ['State 2', 'Test 2'], action = StateChangeAction.TEARDOWN)
    void state2Teardown() {
      state2TeardownCalled = true
    }

    @State(['Test 2'])
    void state3(Map params) {
      state3Called = params
    }
  }

  def setup() {
    interaction = new RequestResponseInteraction()
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction ])
    testResultAcc = Mock(TestResultAccumulator)
    verificationExtension = new PactVerificationStateChangeExtension(pact, interaction, testResultAcc)
  }

  @Unroll
  def 'throws an exception if it does not find a state change method for the provider state'() {
    given:
    def state = new ProviderState('test state')

    when:
    verificationExtension.invokeStateChangeMethods(['getTestClass': { Optional.of(testClass) } ] as ExtensionContext,
      [state], StateChangeAction.SETUP)

    then:
    thrown(MissingStateChangeMethod)

    where:

    testClass << [PactVerificationStateChangeExtensionSpec, TestClass]
  }

  def 'invokes the state change method for the provider state'() {
    given:
    def state = new ProviderState('Test 2', [a: 'A', b: 'B'])
    def testInstance = new TestClass()

    when:
    testInstance.state2Called = false
    testInstance.state2TeardownCalled = false
    testInstance.state3Called = null
    verificationExtension.invokeStateChangeMethods([
      'getTestClass': { Optional.of(TestClass) },
      'getTestInstance': { Optional.of(testInstance) }
    ] as ExtensionContext, [state], StateChangeAction.SETUP)

    then:
    testInstance.state2Called
    testInstance.state3Called == state.params
    !testInstance.state2TeardownCalled
  }

  @SuppressWarnings('ClosureAsLastMethodParameter')
  def 'marks the test as failed if the provider state callback fails'() {
    given:
    def state = new ProviderState('test state')
    interaction.providerStates = [ state ]
    def store = Mock(ExtensionContext.Store)
    def context = Mock(ExtensionContext) {
      getStore(_) >> store
      getRequiredTestClass() >> TestClass
    }
    def target = Mock(TestTarget)
    IProviderVerifier verifier = Mock()
    ValueResolver resolver = Mock()
    ProviderInfo provider = Mock()
    String consumer = 'consumer'
    def verificationContext = new PactVerificationContext(store, context, target, verifier, resolver, provider,
      consumer, interaction, TestResult.Ok.INSTANCE)
    store.get(_) >> verificationContext

    when:
    verificationExtension.beforeTestExecution(context)

    then:
    1 * testResultAcc.updateTestResult(_, interaction, { it instanceof TestResult.Failed })
  }

}
