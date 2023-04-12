package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit.MockServerConfig
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
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

import java.lang.reflect.Method

@SuppressWarnings(['EmptyMethod', 'UnusedMethodParameter', 'UnnecessaryGetter',
  'UnnecessaryParenthesesForMethodCallWithClosure', 'LineLength', 'ExplicitHashSetInstantiation'])
@PactTestFor(providerName = 'PactConsumerTestExtSpecProvider', pactVersion = PactSpecVersion.V3)
class PactConsumerTestExtSpec extends Specification {

  private PactConsumerTestExt testExt
  private Map<String, Object> mockStoreData
  private ExtensionContext.Store mockStore
  private ExtensionContext mockContext
  private Class requiredTestClass
  private Method testMethod

  def testMethodRequestResponsePact(RequestResponsePact pact) { }
  def testMethodMessagePact(MessagePact pact) { }
  def testMethodV4Pact(V4Pact pact) { }
  def testMethodV4MessagePact(V4Pact pact) { }
  def testMethodV4SynchMessagePact(V4Pact pact) { }

  def setup() {
    testExt = new PactConsumerTestExt()

    mockStoreData = [:]
    mockStore = Mock(ExtensionContext.Store) {
      get(_) >> { p -> mockStoreData.get(p[0]) }
      put(_, _) >> { k, v -> mockStoreData.put(k, v) }
    }
    requiredTestClass = PactConsumerTestExtSpec
    mockContext = Mock() {
      getRequiredTestClass() >> { requiredTestClass }
      getTestClass() >> { Optional.ofNullable(requiredTestClass) }
      getTestMethod() >> { Optional.ofNullable(testMethod) }
      getExecutionException() >> Optional.empty()
      getStore(_) >> mockStore
      getRequiredTestInstance() >> { requiredTestClass.newInstance() }
    }
  }

  @Unroll
  def 'supports injecting Pact #model into test methods'() {
    given:
    def parameter = PactConsumerTestExtSpec.getMethod(testMethodName, model).parameters[0]
    def parameterContext = [getParameter: { parameter } ] as ParameterContext
    def providerInfo = new ProviderInfo('test', 'localhost', '0', PactSpecVersion.V3,
      providerType, false)

    def store = [get: { arg ->
      if (arg == 'providers') {
        [new Pair(providerInfo, ['test'])]
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

    model               | providerType               | testMethodName
    RequestResponsePact | ProviderType.SYNCH         | 'testMethodRequestResponsePact'
    MessagePact         | ProviderType.ASYNCH        | 'testMethodMessagePact'
    V4Pact              | ProviderType.SYNCH         | 'testMethodV4Pact'
    V4Pact              | ProviderType.ASYNCH        | 'testMethodV4MessagePact'
    V4Pact              | ProviderType.SYNCH_MESSAGE | 'testMethodV4SynchMessagePact'
  }

  @RestoreSystemProperties
  @SuppressWarnings(['UnnecessaryParenthesesForMethodCallWithClosure', 'UnnecessaryGetter'])
  def 'never overwrites Pacts defined within same class'() {
    given:
    System.setProperty('pact.writer.overwrite', 'true')

    def mockServer = Mockito.mock(BaseMockServer)
    Mockito.when(mockServer.validateMockServerState(Mockito.any())).then {
      new PactVerificationResult.Ok()
    }
    Mockito.when(mockServer.updatePact(Mockito.any())).then {
      it.arguments[0]
    }

    mockStoreData['mockServer:provider'] = new JUnit5MockServerSupport(mockServer)
    mockStoreData['mockServerConfig:provider'] = new MockProviderConfig()
    mockStoreData['providers']  = [new Pair(new ProviderInfo('provider'), ['test'])]

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

  def 'lookupProviderInfo - returns data from the class level PactTestFor annotation'() {
    when:
    def providerInfo = testExt.lookupProviderInfo(mockContext)

    then:
    providerInfo.size() == 1
    providerInfo.first().first.providerName == 'PactConsumerTestExtSpecProvider'
    providerInfo.first().first.pactVersion == PactSpecVersion.V3
    providerInfo.first().second.empty
  }

  static class TestClass {
    @PactTestFor(providerName = 'PactConsumerTestExtSpecMethodProvider', pactVersion = PactSpecVersion.V1)
    def pactTestForMethod() { }
  }

  def 'lookupProviderInfo - returns data from the method level PactTestFor annotation'() {
    given:
    testMethod = TestClass.getMethod('pactTestForMethod')

    when:
    def providerInfo = testExt.lookupProviderInfo(mockContext)

    then:
    providerInfo.size() == 1
    providerInfo.first().first.providerName ==  'PactConsumerTestExtSpecMethodProvider'
    providerInfo.first().first.pactVersion == PactSpecVersion.V1
    providerInfo.first().second.empty
  }

  @PactTestFor(providerName = 'PactConsumerTestExtSpecClassProvider', pactVersion = PactSpecVersion.V3)
  static class TestClass2 {
    @PactTestFor(providerName = 'PactConsumerTestExtSpecMethodProvider')
    def pactTestForMethod() { }
  }

  def 'lookupProviderInfo - returns data from both the method and class level PactTestFor annotation'() {
    given:
    testMethod = TestClass2.getMethod('pactTestForMethod')
    requiredTestClass = TestClass2

    when:
    def providerInfo = testExt.lookupProviderInfo(mockContext)

    then:
    providerInfo.size() == 1
    providerInfo.first().first.providerName ==  'PactConsumerTestExtSpecMethodProvider'
    providerInfo.first().first.pactVersion == PactSpecVersion.V3
    providerInfo.first().second.empty
  }

  @PactTestFor(providerName = 'PactConsumerTestExtSpecClassProvider', pactVersion = PactSpecVersion.V3)
  @MockServerConfig(port = '1234', tls = true)
  static class TestClass3 { }

  def 'lookupProviderInfo - merges data from the class level MockServerConfig annotation'() {
    given:
    requiredTestClass = TestClass3

    when:
    def providerInfo = testExt.lookupProviderInfo(mockContext)

    then:
    providerInfo.size() == 1
    providerInfo.first().first.providerName == 'PactConsumerTestExtSpecClassProvider'
    providerInfo.first().first.pactVersion == PactSpecVersion.V3
    providerInfo.first().first.https
    providerInfo.first().first.port == '1234'
    providerInfo.first().second.empty
  }

  @PactTestFor(providerName = 'PactConsumerTestExtSpecClassProvider', pactVersion = PactSpecVersion.V3)
  static class TestClass4 {
    @PactTestFor(providerName = 'PactConsumerTestExtSpecMethodProvider')
    @MockServerConfig(port = '1235', tls = true)
    def pactTestForMethod() { }
  }

  def 'lookupProviderInfo - merges data from the method level MockServerConfig annotation'() {
    given:
    testMethod = TestClass4.getMethod('pactTestForMethod')
    requiredTestClass = TestClass4

    when:
    def providerInfo = testExt.lookupProviderInfo(mockContext)

    then:
    providerInfo.size() == 1
    providerInfo.first().first.providerName == 'PactConsumerTestExtSpecMethodProvider'
    providerInfo.first().first.pactVersion == PactSpecVersion.V3
    providerInfo.first().first.https
    providerInfo.first().first.port == '1235'
    providerInfo.first().second.empty
  }

  @PactTestFor(providerName = 'TestClassEmptyProviderOnMethod', pactVersion = PactSpecVersion.V3)
  static class TestClassEmptyProviderOnMethod {
    @Pact
    RequestResponsePact pactMethod(PactDslWithProvider builder) { builder.toPact() }

    @PactTestFor(pactMethods = [ 'pactMethod' ])
    def pactTestForMethod() { }
  }

  def 'lookupProviderInfo - do not overwrite the class level values if the method level one is empty'() {
    given:
    testMethod = TestClassEmptyProviderOnMethod.getMethod('pactTestForMethod')
    requiredTestClass = TestClassEmptyProviderOnMethod

    when:
    def providerInfo = testExt.lookupProviderInfo(mockContext)

    then:
    providerInfo.size() == 1
    providerInfo.first().first.providerName == 'TestClassEmptyProviderOnMethod'
    providerInfo.first().first.pactVersion == PactSpecVersion.V3
    providerInfo.first().second == ['pactMethod']
  }

  @PactTestFor(providerName = 'TestClassMultiplePactMethods')
  static class TestClassMultiplePactMethods {
    @Pact
    RequestResponsePact pactMethod1(PactDslWithProvider builder) {
      builder
        .uponReceiving('interaction 1')
        .path('/one')
        .toPact()
    }

    @Pact
    RequestResponsePact pactMethod2(PactDslWithProvider builder) {
      builder
        .uponReceiving('interaction 2')
        .path('/two')
        .toPact()
    }

    @PactTestFor(pactMethods = [ 'pactMethod1', 'pactMethod2' ])
    def pactTestForMethod() { }
  }

  def 'lookupProviderInfo - with multiple pact methods for the same provider'() {
    given:
    testMethod = TestClassMultiplePactMethods.getMethod('pactTestForMethod')
    requiredTestClass = TestClassMultiplePactMethods

    when:
    def providerInfo = testExt.lookupProviderInfo(mockContext)

    then:
    providerInfo.size() == 1
    providerInfo.first().first.providerName == 'TestClassMultiplePactMethods'
    providerInfo.first().second == ['pactMethod1', 'pactMethod2']
  }

  def 'mockServerConfigured - returns false when there are no MockServerConfig annotations'() {
    expect:
    !testExt.mockServerConfigured(mockContext)
  }

  def 'mockServerConfigured - returns true when there is a MockServerConfig annotation on the test class'() {
    given:
    requiredTestClass = TestClass3

    expect:
    testExt.mockServerConfigured(mockContext)
  }

  def 'mockServerConfigured - returns true when there is a MockServerConfig annotation on the test method'() {
    given:
    requiredTestClass = TestClass4
    testMethod = TestClass4.getMethod('pactTestForMethod')

    expect:
    testExt.mockServerConfigured(mockContext)
  }

  @MockServerConfig(providerName = 'a', port = '1236')
  @MockServerConfig(providerName = 'b', port = '1237')
  static class TestClass5 {
    def pactTestForMethod() { }
  }

  static class TestClass6 {
    @MockServerConfig(providerName = 'a', port = '1238')
    @MockServerConfig(providerName = 'b', port = '1239')
    def pactTestForMethod() { }

    @PactIgnore
    def nonPactTestMethod() { }
  }

  def 'mockServerConfigured - returns true when there are multiple MockServerConfig annotations on the test class'() {
    given:
    requiredTestClass = TestClass5

    expect:
    testExt.mockServerConfigured(mockContext)
  }

  def 'mockServerConfigured - returns true when there are multiple MockServerConfig annotations on the test method'() {
    given:
    requiredTestClass = TestClass6
    testMethod = TestClass6.getMethod('pactTestForMethod')

    expect:
    testExt.mockServerConfigured(mockContext)
  }

  def 'mockServerConfigFromAnnotation - returns null when there are no MockServerConfig annotations'() {
    expect:
    !testExt.mockServerConfigFromAnnotation(mockContext, null)
  }

  def 'mockServerConfigFromAnnotation - returns MockServerConfig annotation on the test class'() {
    given:
    requiredTestClass = TestClass3

    when:
    def config = testExt.mockServerConfigFromAnnotation(mockContext, null)

    then:
    config.port == 1234
  }

  def 'mockServerConfigFromAnnotation - returns MockServerConfig annotation on the test method'() {
    given:
    requiredTestClass = TestClass4
    testMethod = TestClass4.getMethod('pactTestForMethod')

    when:
    def config = testExt.mockServerConfigFromAnnotation(mockContext, null)

    then:
    config.port == 1235
  }

  def 'mockServerConfigFromAnnotation - returns first MockServerConfig annotation on the test class when there is no provider info'() {
    given:
    requiredTestClass = TestClass5

    when:
    def config = testExt.mockServerConfigFromAnnotation(mockContext, null)

    then:
    config.port == 1236
  }

  def 'mockServerConfigFromAnnotation - returns first MockServerConfig annotation on the test method when there is no provider info'() {
    given:
    requiredTestClass = TestClass6
    testMethod = TestClass6.getMethod('pactTestForMethod')

    when:
    def config = testExt.mockServerConfigFromAnnotation(mockContext, null)

    then:
    config.port == 1238
  }

  def 'mockServerConfigFromAnnotation - returns MockServerConfig annotation on the test class for the given provider'() {
    given:
    requiredTestClass = TestClass5
    def provider = new ProviderInfo('b')

    when:
    def config = testExt.mockServerConfigFromAnnotation(mockContext, provider)

    then:
    config.port == 1237
  }

  def 'mockServerConfigFromAnnotation - returns first MockServerConfig annotation on the test method for the given provider'() {
    given:
    requiredTestClass = TestClass6
    testMethod = TestClass6.getMethod('pactTestForMethod')
    def provider = new ProviderInfo('b')

    when:
    def config = testExt.mockServerConfigFromAnnotation(mockContext, provider)

    then:
    config.port == 1239
  }

  def 'ignoredTest - returns false for a normal test method'() {
    given:
    requiredTestClass = TestClass6
    testMethod = TestClass6.getMethod('pactTestForMethod')

    expect:
    !testExt.ignoredTest(mockContext)
  }

  def 'ignoredTest - returns true for a test method annotated with PactIgnore'() {
    given:
    requiredTestClass = TestClass6
    testMethod = TestClass6.getMethod('nonPactTestMethod')

    expect:
    testExt.ignoredTest(mockContext)
  }

  class TestSetupPactFor {
    @Pact(consumer = 'consumer1')
    RequestResponsePact pactMethod1(PactDslWithProvider builder) {
      builder
        .uponReceiving('interaction 1')
        .path('/one')
        .willRespondWith()
        .toPact()
    }

    @Pact(consumer = 'consumer1')
    RequestResponsePact pactMethod2(PactDslWithProvider builder) {
      builder
        .uponReceiving('interaction 2')
        .path('/two')
        .willRespondWith()
        .toPact()
    }

    @PactTestFor
    def testMethod() { }
  }

  def 'setupPactForTest - pact methods is empty'() {
    given:
    requiredTestClass = TestSetupPactFor
    testMethod = TestSetupPactFor.getMethod('testMethod')
    def provider = new ProviderInfo('setupPactForTest', '', '', PactSpecVersion.V3)
    mockStoreData['executedFragments'] = new HashSet()

    expect:
    testExt.setupPactForTest(provider, [], mockContext).interactions*.description == ['interaction 1']
  }

  def 'setupPactForTest - pact methods has one entry'() {
    given:
    requiredTestClass = TestSetupPactFor
    testMethod = TestSetupPactFor.getMethod('testMethod')
    def provider = new ProviderInfo('setupPactForTest', '', '', PactSpecVersion.V3)
    mockStoreData['executedFragments'] = new HashSet()

    expect:
    testExt.setupPactForTest(provider, ['pactMethod2'], mockContext).interactions*.description == ['interaction 2']
  }

  def 'setupPactForTest - pact methods has more than one entry'() {
    given:
    requiredTestClass = TestSetupPactFor
    testMethod = TestSetupPactFor.getMethod('testMethod')
    def provider = new ProviderInfo('setupPactForTest', '', '', PactSpecVersion.V3)
    mockStoreData['executedFragments'] = new HashSet()

    expect:
    testExt.setupPactForTest(provider, ['pactMethod1', 'pactMethod2'], mockContext).interactions*.description ==
      ['interaction 1', 'interaction 2']
  }
}
