package au.com.dius.pact.provider.spring

import au.com.dius.pact.provider.junit.Provider
import au.com.dius.pact.provider.junit.RestPactRunner
import au.com.dius.pact.provider.junit.State
import au.com.dius.pact.provider.junit.StateChangeAction
import au.com.dius.pact.provider.junit.loader.PactFilter
import au.com.dius.pact.provider.junit.loader.PactFolder
import au.com.dius.pact.provider.junit.target.TestTarget
import au.com.dius.pact.provider.spring.target.MockMvcTarget
import groovy.util.logging.Slf4j
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController {

  @RequestMapping(path = ['/user-service/users'], produces = ['application/json'])
  @ResponseStatus(HttpStatus.CREATED)
  Map users() {
    [id: 100]
  }

}

@RunWith(RestPactRunner)
@Provider('userservice')
@PactFolder('pacts-for-filter-test')
@PactFilter('provider accepts a new person')
@SuppressWarnings(['PublicInstanceField', 'JUnitPublicNonTestMethod', 'JUnitPublicField', 'EmptyMethod'])
@Slf4j
class SpringFilteredTest {

  @TestTarget
  public final MockMvcTarget target = new MockMvcTarget()

  @Before
  void setup() {
    target.setControllers(new TestController())
  }

  @State(value = 'provider accepts a new person', action = StateChangeAction.SETUP)
  void toCreatePersonState() {
    log.debug('State change method called')
  }

  @State(value = 'provider accepts a new person', action = StateChangeAction.TEARDOWN)
  void teardownPersonState() {
    log.debug('State change teardown method called')
  }

}
