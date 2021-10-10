package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.support.BuiltToolConfig
import groovy.json.JsonSlurper
import kotlin.Pair
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
  def testMethodV4Pact(V4Pact pact) { }
  def testMethodV4MessagePact(V4Pact pact) { }
  def testMethodV4SynchMessagePact(V4Pact pact) { }

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
      if (arg == 'providers') {
        [new Pair(providerInfo, 'test')]
      } else if (model.isAssignableFrom(V4Pact)) {
        model.newInstance(new Consumer(), new Provider(), [])
      } else {
        model.newInstance(new Provider(), new Consumer(), [])
      }
    } ] as ExtensionContext.Store
    def extensionContext = [getStore: { store } ] as ExtensionContext

    expect:
    testExt.supportsParameter(parameterContext, extensionContext)
    testExt.resolveParameter(parameterContext, extensionContext).class == model

    where:

    model               | providerType               | testMethod
    RequestResponsePact | ProviderType.SYNCH         | 'testMethodRequestResponsePact'
    MessagePact         | ProviderType.ASYNCH        | 'testMethodMessagePact'
    V4Pact              | ProviderType.SYNCH         | 'testMethodV4Pact'
    V4Pact              | ProviderType.ASYNCH        | 'testMethodV4MessagePact'
    V4Pact              | ProviderType.SYNCH_MESSAGE | 'testMethodV4SynchMessagePact'
  }

  @RestoreSystemProperties
  def 'never overwrites Pacts defined within same class'() {
    given:
    System.setProperty('pact.writer.overwrite', 'true')

    def mockServer = Mockito.mock(BaseMockServer)
    Mockito.when(mockServer.validateMockServerState(Mockito.any())).then {
      new PactVerificationResult.Ok()
    }
    def mockStoreData = [
      'mockServer:provider': new JUnit5MockServerSupport(mockServer),
      'mockServerConfig:provider': new MockProviderConfig(),
      'providers': [new Pair(new ProviderInfo('provider'), 'test')]
    ]
    def mockStore = Mock(ExtensionContext.Store) {
      get(_) >> { p -> mockStoreData.get(p[0]) }
      put(_, _) >> { k, v -> mockStoreData.put(k, v) }
    }
    ExtensionContext mockContext = Mock() {
      getRequiredTestClass() >> PactConsumerTestExtSpec
      getTestClass() >> Optional.of(PactConsumerTestExtSpec)
      getExecutionException() >> Optional.empty()
      getStore(_) >> mockStore
    }

    def provider = new Provider('provider')
    def consumer = new Consumer('consumer')
    def first = new RequestResponsePact(provider, consumer, [new RequestResponseInteraction('first')])
    def second = new RequestResponsePact(provider, consumer, [new RequestResponseInteraction('second')])

    when:
    testExt.beforeAll(mockContext)
    mockStoreData['pact:provider'] = first  // normally set by testExt.resolveParameter()
    testExt.afterTestExecution(mockContext)
    mockStoreData['pact:provider'] = second  // normally set by testExt.resolveParameter()
    testExt.afterTestExecution(mockContext)
    testExt.afterAll(mockContext)
    def pactFile = new File("${BuiltToolConfig.pactDirectory}/consumer-provider.json")
    def json = new JsonSlurper().parse(pactFile)

    then:
    json.metadata.pactSpecification.version == '4.0'
    json.interactions[0].description == 'first'
    json.interactions[1].description == 'second'
  }
}
