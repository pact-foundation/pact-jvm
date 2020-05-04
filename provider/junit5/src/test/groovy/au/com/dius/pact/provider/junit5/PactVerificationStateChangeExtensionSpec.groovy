package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.pactbroker.TestResult
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.TestResultAccumulator
import au.com.dius.pact.provider.junit.MissingStateChangeMethod
import au.com.dius.pact.provider.junit.State
import au.com.dius.pact.provider.junit.StateChangeAction
import au.com.dius.pact.core.support.expressions.ValueResolver
import org.junit.jupiter.api.extension.ExtensionContext
import spock.lang.Specification
import spock.lang.Unroll

class PactVerificationStateChangeExtensionSpec extends Specification {

  private PactVerificationStateChangeExtension verificationExtension
  Interaction interaction
  private TestResultAccumulator testResultAcc
  RequestResponsePact pact
  private PactVerificationContext pactContext
  private ExtensionContext testContext
  private ExtensionContext.Store store

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

  private TestClass testInstance

  def setup() {
    interaction = new RequestResponseInteraction('test')
    pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction ])
    testResultAcc = Mock(TestResultAccumulator)
    verificationExtension = new PactVerificationStateChangeExtension(pact, interaction, testResultAcc)
    testInstance = new TestClass()
    testContext = [
      'getTestClass': { Optional.of(TestClass) },
      'getTestInstance': { Optional.of(testInstance) }
    ] as ExtensionContext
    store = [:] as ExtensionContext.Store
    pactContext = new PactVerificationContext(store, testContext, 'test', interaction)
  }

  @Unroll
  def 'throws an exception if it does not find a state change method for the provider state'() {
    given:
    def state = new ProviderState('test state')

    when:
    verificationExtension.invokeStateChangeMethods(testContext, pactContext, [state], StateChangeAction.SETUP)

    then:
    thrown(MissingStateChangeMethod)

    where:

    testClass << [PactVerificationStateChangeExtensionSpec, TestClass]
  }

  def 'invokes the state change method for the provider state'() {
    given:
    def state = new ProviderState('Test 2', [a: 'A', b: 'B'])

    when:
    testInstance.state2Called = false
    testInstance.state2TeardownCalled = false
    testInstance.state3Called = null
    verificationExtension.invokeStateChangeMethods(testContext, pactContext, [state], StateChangeAction.SETUP)

    then:
    testInstance.state2Called
    testInstance.state3Called == state.params
    !testInstance.state2TeardownCalled
  }

  @SuppressWarnings('ClosureAsLastMethodParameter')
  def 'marks the test as failed if the provider state callback fails'() {
    given:
    def state = new ProviderState('test state')
    def interaction = new RequestResponseInteraction('test', [ state ])
    def store = Mock(ExtensionContext.Store)
    def context = Mock(ExtensionContext) {
      getStore(_) >> store
      getRequiredTestClass() >> TestClass
      getRequiredTestInstance() >> testInstance
    }
    def target = Mock(TestTarget)
    IProviderVerifier verifier = Mock()
    ValueResolver resolver = Mock()
    ProviderInfo provider = Mock()
    String consumer = 'consumer'
    def verificationContext = new PactVerificationContext(store, context, target, verifier, resolver, provider,
      consumer, interaction, TestResult.Ok.INSTANCE)
    store.get(_) >> verificationContext
    verificationExtension = new PactVerificationStateChangeExtension(pact, interaction, testResultAcc)

    when:
    verificationExtension.beforeTestExecution(context)

    then:
    thrown(AssertionError)
    verificationContext.testExecutionResult instanceof TestResult.Failed
  }

}
