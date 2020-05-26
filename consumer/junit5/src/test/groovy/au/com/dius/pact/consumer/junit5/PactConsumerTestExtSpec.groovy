package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.PactConsumerConfig
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.messaging.MessagePact
import groovy.json.JsonSlurper
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.mockito.Mockito
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

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
      providerType, false)

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

  @RestoreSystemProperties
  def 'never overwrites Pacts defined within same class'() {
    given:
    System.setProperty('pact.writer.overwrite', 'true')

    def mockServer = Mockito.mock(BaseMockServer)
    Mockito.when(mockServer.validateMockServerState(Mockito.any())).then {
      new PactVerificationResult.Ok()
    }
    def mockStore = [
      'mockServer': new JUnit5MockServerSupport(mockServer),
      'mockServerConfig': new MockProviderConfig(),
      'providerInfo': new ProviderInfo()
    ]
    def mockContext = [
      'getTestClass': { Optional.of(Object) },
      'getExecutionException': { Optional.empty() },
      'getStore': {
        [
          'get': { mockStore.get(it) },
          'put': { k, v -> mockStore.put(k, v) }
        ] as ExtensionContext.Store
      }
    ] as ExtensionContext

    def provider = new Provider('provider')
    def consumer = new Consumer('consumer')
    def first = new RequestResponsePact(provider, consumer, [new RequestResponseInteraction('first')])
    def second = new RequestResponsePact(provider, consumer, [new RequestResponseInteraction('second')])

    when:
    testExt.beforeAll(mockContext)
    mockStore['pact'] = first  // normally set by testExt.resolveParameter()
    testExt.afterTestExecution(mockContext)
    mockStore['pact'] = second  // normally set by testExt.resolveParameter()
    testExt.afterTestExecution(mockContext)
    testExt.afterAll(mockContext)
    def pactFile = new File("${PactConsumerConfig.pactDirectory}/consumer-provider.json")
    def json = new JsonSlurper().parse(pactFile)

    then:
    json.metadata.pactSpecification.version == '3.0.0'
    json.interactions[0].description == 'first'
    json.interactions[1].description == 'second'
  }

}
