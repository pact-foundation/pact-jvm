package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.RequestResponsePact
import au.com.dius.pact.model.v3.messaging.MessagePact
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings(['EmptyMethod', 'UnusedMethodParameter'])
class PactConsumerTestExtSpec extends Specification {

  private PactConsumerTestExt testExt

  def testMethodRequestResponsePact(RequestResponsePact pact) { }
  def testMethodMessagePact(MessagePact pact) { }

  def setup() {
    testExt = new PactConsumerTestExt()
  }

  @Unroll
  def 'supports injecting Pact #model into test methods'() {
    given:
    def parameter = PactConsumerTestExtSpec.getMethod(testMethod, model).parameters[0]
    def parameterContext = [getParameter: { parameter } ] as ParameterContext
    def providerInfo = new ProviderInfo('test', 'localhost', '0', PactSpecVersion.V3,
      providerType)

    def store = [get: { arg ->
      arg == 'providerInfo' ? providerInfo : model.newInstance(new Provider(), new Consumer(), [])
    } ] as ExtensionContext.Store
    def extensionContext = [getStore: { store } ] as ExtensionContext

    expect:
    testExt.supportsParameter(parameterContext, extensionContext)
    testExt.resolveParameter(parameterContext, extensionContext).class == model

    where:

    model               | providerType        | testMethod
    RequestResponsePact | ProviderType.SYNCH  | 'testMethodRequestResponsePact'
    MessagePact         | ProviderType.ASYNCH | 'testMethodMessagePact'
  }

}
