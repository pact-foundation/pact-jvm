package au.com.dius.pact.provider.junit

import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.TargetRequestFilter
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.junitsupport.target.TestTarget
import com.github.restdriver.clientdriver.ClientDriverRule
import groovy.util.logging.Slf4j
import org.apache.http.HttpRequest
import org.junit.Before
import org.junit.ClassRule
import org.junit.runner.RunWith

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo
import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST
import static org.hamcrest.Matchers.equalTo

@RunWith(PactRunner)
@Provider('providerInjectedHeaders')
@PactFolder('pacts')
@Slf4j
class InjectedHeadersContractTest {
  @ClassRule
  @SuppressWarnings('FieldName')
  public static final ClientDriverRule embeddedService = new ClientDriverRule(8332)

  @TestTarget
  @SuppressWarnings(['PublicInstanceField', 'JUnitPublicField'])
  public final Target target = new HttpTarget(8332)

  @Before
  void before() {
    embeddedService.addExpectation(
      onRequestTo('/accounts').withMethod(POST)
        .withHeader('X-ContractTest', equalTo('true')),

      giveEmptyResponse().withStatus(201)
        .withHeader('Location', 'http://localhost:8332/accounts/1234')
    )
  }

  @TargetRequestFilter
  void exampleRequestFilter(HttpRequest request) {
    request.addHeader('X-ContractTest', 'true')
  }

  @State(value = 'an active account exists', comment = 'I\'m a comment')
  Map<String, Object> createAccount() {
    [
      port: 8332,
      accountId: '1234'
    ]
  }
}
