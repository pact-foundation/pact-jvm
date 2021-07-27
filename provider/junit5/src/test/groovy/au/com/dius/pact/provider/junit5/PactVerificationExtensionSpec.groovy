package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.PactBrokerSource
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.TestResultAccumulator
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.extension.ExtensionContext
import spock.lang.Specification

class PactVerificationExtensionSpec extends Specification {

  def 'updateTestResult uses the original pact when pact is filtered '() {
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
    def mockValueResolver = Mock(ValueResolver)

    def interaction1 = new RequestResponseInteraction('interaction1', [], new Request(), new Response())
    def interaction2 = new RequestResponseInteraction('interaction2', [], new Request(), new Response())
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [interaction1, interaction2])
    def filteredPact = new FilteredPact(pact, { it.description == 'interaction1' })
    PactBrokerSource pactSource = new PactBrokerSource('localhost', '80', 'http')

    context = new PactVerificationContext(store, extContext, Stub(TestTarget), Stub(IProviderVerifier),
      Stub(ValueResolver), Stub(IProviderInfo), Stub(IConsumerInfo), interaction1, [])

    PactVerificationExtension extension = new PactVerificationExtension(filteredPact, pactSource, interaction1,
      'service', 'consumer', mockValueResolver)
    extension.testResultAccumulator = Mock(TestResultAccumulator)

    when:
    extension.afterTestExecution(extContext)

    then:
    1 * extension.testResultAccumulator.updateTestResult(pact, interaction1, [], pactSource, mockValueResolver) >>
      new Ok(true)
  }

  def 'updateTestResult uses the pact itself when pact is not filtered '() {
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
    def mockValueResolver = Mock(ValueResolver)

    def interaction1 = new RequestResponseInteraction('interaction1', [], new Request(), new Response())
    def interaction2 = new RequestResponseInteraction('interaction2', [], new Request(), new Response())
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [interaction1, interaction2])
    PactBrokerSource pactSource = new PactBrokerSource('localhost', '80', 'http')

    context = new PactVerificationContext(store, extContext, Stub(TestTarget), Stub(IProviderVerifier),
      Stub(ValueResolver), Stub(IProviderInfo), Stub(IConsumerInfo), interaction1, [])

    PactVerificationExtension extension = new PactVerificationExtension(pact, pactSource, interaction1,
      'service', 'consumer', mockValueResolver)
    extension.testResultAccumulator = Mock(TestResultAccumulator)

    when:
    extension.afterTestExecution(extContext)

    then:
    1 * extension.testResultAccumulator.updateTestResult(pact, interaction1, [], pactSource, mockValueResolver)
  }

  def 'if updateTestResult fails, throw an exception'() {
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
    def mockValueResolver = Mock(ValueResolver)

    def interaction1 = new RequestResponseInteraction('interaction1')
    def interaction2 = new RequestResponseInteraction('interaction2')
    PactBrokerSource pactSource = new PactBrokerSource('localhost', '80', 'http')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [interaction1, interaction2 ], [:], pactSource)

    context = new PactVerificationContext(store, extContext, Stub(TestTarget), Stub(IProviderVerifier),
      Stub(ValueResolver), Stub(IProviderInfo), Stub(IConsumerInfo), interaction1, [])

    PactVerificationExtension extension = new PactVerificationExtension(pact, pactSource, interaction1,
      'service', 'consumer', mockValueResolver)
    extension.testResultAccumulator = Mock(TestResultAccumulator)

    when:
    extension.afterTestExecution(extContext)

    then:
    1 * extension.testResultAccumulator.updateTestResult(pact, interaction1, [], pactSource, mockValueResolver) >>
      new Err(['failed'])
    def exception = thrown(AssertionError)
    exception.message == 'Failed to update the test results: failed'
  }
}
