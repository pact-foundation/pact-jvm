package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import org.junit.jupiter.api.extension.ExtensionContext
import spock.lang.Issue
import spock.lang.Specification

@SuppressWarnings('UnnecessaryGetter')
class PactVerificationContextSpec extends Specification {

  def 'sets the test result to an error result if the test fails with an exception'() {
    given:
    PactVerificationContext context
    ExtensionContext.Store store = Stub {
      get(_) >> { args ->
        if (args[0] == 'interactionContext') {
          context
        }
      }
    }
    ExtensionContext extContext = Stub {
      getStore(_) >> store
    }
    TestTarget target = Stub {
      executeInteraction(_, _) >> { throw new IOException('Boom!') }
    }
    IProviderVerifier verifier = Stub()
    ValueResolver valueResolver = Stub()
    IProviderInfo provider = Stub {
      getName() >> 'Stub'
    }
    IConsumerInfo consumer = new ConsumerInfo('Test')
    Interaction interaction = new RequestResponseInteraction('Test Interaction', [], new Request(),
      new Response(), '12345')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [interaction ])
    List<VerificationResult> testResults = []

    context = new PactVerificationContext(store, extContext, target, verifier, valueResolver,
      provider, consumer, interaction, pact, testResults)

    when:
    context.verifyInteraction()

    then:
    thrown(AssertionError)
    context.testExecutionResult[0] instanceof VerificationResult.Failed
    context.testExecutionResult[0].description == 'Request to provider failed with an exception'
    context.testExecutionResult[0].failures.size() == 1
    context.testExecutionResult[0].failures['12345'][0] instanceof VerificationFailureType.ExceptionFailure
  }

  def 'only throw an exception if there are non-pending failures'() {
    given:
    PactVerificationContext context
    ExtensionContext.Store store = Stub {
      get(_) >> { args ->
        if (args[0] == 'interactionContext') {
          context
        }
      }
    }
    ExtensionContext extContext = Stub {
      getStore(_) >> store
    }
    TestTarget target = Stub {
      executeInteraction(_, _) >> { throw new IOException('Boom!') }
    }
    IProviderVerifier verifier = Stub()
    ValueResolver valueResolver = Stub()
    IProviderInfo provider = Stub {
      getName() >> 'Stub'
    }
    IConsumerInfo consumer = Mock(IConsumerInfo) {
      getName() >> 'test'
      getPending() >> true
    }
    Interaction interaction = new RequestResponseInteraction('Test Interaction', [], new Request(),
      new Response(), '12345')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction ])
    List<VerificationResult> testResults = []

    context = new PactVerificationContext(store, extContext, target, verifier, valueResolver,
      provider, consumer, interaction, pact, testResults)

    when:
    context.verifyInteraction()

    then:
    noExceptionThrown()
    context.testExecutionResult[0] instanceof VerificationResult.Failed
    context.testExecutionResult[0].description == 'Request to provider failed with an exception'
    context.testExecutionResult[0].failures.size() == 1
    context.testExecutionResult[0].failures['12345'][0] instanceof VerificationFailureType.ExceptionFailure
  }

  @Issue('#1573')
  def 'support pending flag with async message interactions'() {
    given:
    PactVerificationContext context
    ExtensionContext.Store store = Stub {
      get(_) >> { args ->
        if (args[0] == 'interactionContext') {
          context
        }
      }
    }
    ExtensionContext extContext = Stub {
      getStore(_) >> store
    }
    TestTarget target = Stub {
      executeInteraction(_, _) >> { throw new IOException('Boom!') }
    }
    IProviderVerifier verifier = Mock()
    ValueResolver valueResolver = Stub()
    IProviderInfo provider = Stub {
      getName() >> 'Stub'
      getVerificationType() >> PactVerification.ANNOTATED_METHOD
    }
    IConsumerInfo consumer = Mock(IConsumerInfo) {
      getName() >> 'test'
      getPending() >> true
    }
    Interaction interaction = new Message('Test Interaction')
    def pact = new MessagePact(new Provider(), new Consumer(), [interaction])
    List<VerificationResult> testResults = []

    context = new PactVerificationContext(store, extContext, target, verifier, valueResolver,
      provider, consumer, interaction, pact, testResults)

    when:
    context.verifyInteraction()

    then:
    1 * verifier.verifyResponseByInvokingProviderMethods(provider, consumer, interaction,
      interaction.description, [:], true, _) >> new VerificationResult.Ok()
  }

  def 'currentTarget - returns the current target if it supports the interaction'() {
    given:
    def expectedTarget = new HttpTestTarget()
    ExtensionContext.Store store = Stub()
    ExtensionContext extContext = Stub()
    IProviderVerifier verifier = Mock()
    ValueResolver valueResolver = Stub()
    IProviderInfo provider = Stub()
    IConsumerInfo consumer = Stub()
    Interaction interaction = new RequestResponseInteraction('test')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [interaction])
    List<VerificationResult> testResults = []

    def context = new PactVerificationContext(store, extContext, expectedTarget, verifier, valueResolver,
      provider, consumer, interaction, pact, testResults)

    when:
    def result = context.currentTarget()

    then:
    result == expectedTarget
  }

  @SuppressWarnings('LineLength')
  def 'currentTarget - searches for a target in the additional ones if the current target does not support the interaction'() {
    given:
    def expectedTarget = new HttpTestTarget()
    ExtensionContext.Store store = Stub()
    ExtensionContext extContext = Stub()
    IProviderVerifier verifier = Mock()
    ValueResolver valueResolver = Stub()
    IProviderInfo provider = Stub()
    IConsumerInfo consumer = Stub()
    Interaction interaction = new RequestResponseInteraction('test')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [interaction])
    List<VerificationResult> testResults = []
    TestTarget otherTarget = Mock {
      supportsInteraction(_) >> false
    }

    def context = new PactVerificationContext(store, extContext, new MessageTestTarget(), verifier, valueResolver,
      provider, consumer, interaction, pact, testResults)
    context.addAdditionalTarget(otherTarget)
    context.addAdditionalTarget(expectedTarget)

    when:
    def result = context.currentTarget()

    then:
    result == expectedTarget
  }

  def 'currentTarget - returns null if no target can be found that supports the interaction'() {
    given:
    ExtensionContext.Store store = Stub()
    ExtensionContext extContext = Stub()
    IProviderVerifier verifier = Mock()
    ValueResolver valueResolver = Stub()
    IProviderInfo provider = Stub()
    IConsumerInfo consumer = Stub()
    Interaction interaction = new RequestResponseInteraction('test')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [interaction])
    List<VerificationResult> testResults = []
    TestTarget otherTarget = Mock {
      supportsInteraction(_) >> false
    }

    def context = new PactVerificationContext(store, extContext, new MessageTestTarget(), verifier, valueResolver,
      provider, consumer, interaction, pact, testResults)
    context.addAdditionalTarget(otherTarget)

    when:
    def result = context.currentTarget()

    then:
    result == null
  }
}
