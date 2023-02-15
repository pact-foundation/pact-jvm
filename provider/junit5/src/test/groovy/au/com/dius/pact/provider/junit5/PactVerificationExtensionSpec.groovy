package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactBrokerSource
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.RequestData
import au.com.dius.pact.provider.RequestDataToBeVerified
import au.com.dius.pact.provider.TestResultAccumulator
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.HttpRequest
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification

import java.lang.reflect.Parameter

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@SuppressWarnings('UnnecessaryGetter')
class PactVerificationExtensionSpec extends Specification {
  @Shared PactVerificationContext context
  PactVerificationExtension extension
  @Shared ExtensionContext.Store store
  @Shared ExtensionContext extContext
  @Shared Map<String, Object> contextMap
  ValueResolver mockValueResolver
  @Shared RequestResponseInteraction interaction1, interaction2
  @Shared RequestResponsePact pact
  PactBrokerSource pactSource
  @Shared ClassicHttpRequest classicHttpRequest
  @Shared ProviderVerifier verifier
  @Shared RequestDataToBeVerified data

  def setupSpec() {
    verifier = Mock(ProviderVerifier)
    store = Stub {
      get(_) >> { args ->
        if (args[0] == 'interactionContext') {
          context
        } else {
          contextMap[args[0]]
        }
      }
      put(_, _) >> { args -> contextMap[args[0]] = args[1]  }
    }

    extContext = Stub {
      getStore(_) >> store
    }
    interaction1 = new RequestResponseInteraction('interaction1', [], new Request(), new Response())
    interaction2 = new RequestResponseInteraction('interaction2', [], new Request(), new Response())
    pact = new RequestResponsePact(new Provider(), new Consumer(), [interaction1, interaction2])
    classicHttpRequest = Mock(ClassicHttpRequest)
    context = new PactVerificationContext(store, extContext, Stub(TestTarget), Stub(IProviderVerifier),
      Stub(ValueResolver), Stub(IProviderInfo), Stub(IConsumerInfo), interaction1, pact, [])
    data = new RequestDataToBeVerified(OptionalBody.empty(), [:])
  }

  def setup() {
    mockValueResolver = Mock(ValueResolver)
    pactSource = new PactBrokerSource('localhost', '80', 'http')
    contextMap = [
      httpRequest: classicHttpRequest,
      verifier: verifier
    ]
  }

  def 'updateTestResult uses the original pact when pact is filtered '() {
    given:
    def filteredPact = new FilteredPact(pact, { it.description == 'interaction1' })
    PactBrokerSource pactSource = new PactBrokerSource('localhost', '80', 'http')

    extension = new PactVerificationExtension(filteredPact, pactSource, interaction1, 'service', 'consumer',
      mockValueResolver)
    extension.testResultAccumulator = Mock(TestResultAccumulator)

    when:
    extension.afterTestExecution(extContext)

    then:
    1 * extension.testResultAccumulator.updateTestResult(pact, interaction1, [], pactSource, mockValueResolver) >>
      new Result.Ok(true)
  }

  def 'updateTestResult uses the pact itself when pact is not filtered '() {
    given:
    extension = new PactVerificationExtension(pact, pactSource, interaction1,
      'service', 'consumer', mockValueResolver)
    extension.testResultAccumulator = Mock(TestResultAccumulator)

    when:
    extension.afterTestExecution(extContext)

    then:
    1 * extension.testResultAccumulator.updateTestResult(pact, interaction1, [], pactSource, mockValueResolver)
  }

  def 'if updateTestResult fails, throw an exception'() {
    given:
    pact = new RequestResponsePact(new Provider(), new Consumer(), [interaction1, interaction2], [:], pactSource)

    extension = new PactVerificationExtension(pact, pactSource, interaction1,
      'service', 'consumer', mockValueResolver)
    extension.testResultAccumulator = Mock(TestResultAccumulator)

    when:
    extension.afterTestExecution(extContext)

    then:
    1 * extension.testResultAccumulator.updateTestResult(pact, interaction1, [], pactSource, mockValueResolver) >>
      new Result.Err(['failed'])
    def exception = thrown(AssertionError)
    exception.message == 'Failed to update the test results: failed'
  }

  @Issue('#1572')
  def 'beforeEach method passes the property resolver on to the verification context'() {
    given:
    pact = new RequestResponsePact(new Provider(), new Consumer(), [interaction1], [:], pactSource)

    extension = new PactVerificationExtension(pact, pactSource, interaction1,
      'service', 'consumer', mockValueResolver)

    when:
    extension.beforeEach(extContext)

    then:
    contextMap['interactionContext'].valueResolver == mockValueResolver
  }

  def 'supports parameter test'() {
    given:
    context.target = target
    extension = new PactVerificationExtension(pact, pactSource, interaction1, 'service', 'consumer',
      mockValueResolver)
    Parameter parameter = mock(Parameter)
    when(parameter.getType()).thenReturn(parameterType)
    ParameterContext parameterContext = Stub {
      getParameter() >> parameter
    }

    expect:
    extension.supportsParameter(parameterContext, extContext) == result

    where:

    parameterType           | target                  | result
    Pact                    | new HttpTestTarget()    | true
    Interaction             | new HttpTestTarget()    | true
    ClassicHttpRequest      | new HttpTestTarget()    | true
    ClassicHttpRequest      | new HttpsTestTarget()   | true
    ClassicHttpRequest      | new MessageTestTarget() | false
    HttpRequest             | new HttpTestTarget()    | true
    HttpRequest             | new HttpsTestTarget()   | true
    HttpRequest             | new MessageTestTarget() | false
    PactVerificationContext | new HttpTestTarget()    | true
    ProviderVerifier        | new HttpTestTarget()    | true
    String                  | new HttpTestTarget()    | false
    RequestData             | new HttpTestTarget()    | false
    RequestData             | new PluginTestTarget()  | true
  }

  def 'resolve parameter test'() {
    given:
    extension = new PactVerificationExtension(pact, pactSource, interaction1, 'service', 'consumer',
      mockValueResolver)
    Parameter parameter = mock(Parameter)
    when(parameter.getType()).thenReturn(parameterType)
    ParameterContext parameterContext = Stub {
      getParameter() >> parameter
    }

    contextMap['request'] = data

    expect:
    extension.resolveParameter(parameterContext, extContext) == result

    where:

    parameterType               | result
    Pact                        | pact
    Interaction                 | interaction1
    ClassicHttpRequest          | classicHttpRequest
    HttpRequest                 | classicHttpRequest
    PactVerificationContext     | context
    ProviderVerifier            | verifier
    String                      | null
    RequestData                 | data
  }
}
