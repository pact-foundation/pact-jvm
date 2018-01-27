package au.com.dius.pact.provider.spring

import au.com.dius.pact.provider.junit.Provider
import au.com.dius.pact.provider.junit.RestPactRunner
import au.com.dius.pact.provider.junit.State
import au.com.dius.pact.provider.junit.loader.PactFilter
import au.com.dius.pact.provider.junit.loader.PactFolder
import au.com.dius.pact.provider.junit.target.TestTarget
import au.com.dius.pact.provider.spring.target.MockMvcTarget
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
class SpringFilteredTest {

  @TestTarget
  public final MockMvcTarget target = new MockMvcTarget()

  @Before
  void setup() {
    target.setControllers(new TestController())
  }

  @State('provider accepts a new person')
  void toCreatePersonState() {
    // Yes, I'm an empty method
  }

}
