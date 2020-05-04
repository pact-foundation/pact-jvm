package au.com.dius.pact.provider.spring

import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.StateChangeAction
import au.com.dius.pact.provider.junitsupport.loader.PactFilter
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.junitsupport.target.TestTarget
import au.com.dius.pact.provider.spring.target.SpringBootHttpTarget
import au.com.dius.pact.provider.spring.testspringbootapp.TestApplication
import groovy.util.logging.Slf4j
import org.junit.AfterClass
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext

@RunWith(SpringRestPactRunner)
@Provider('Books-Service')
@Consumer('Readers-Service')
@PactFilter('book-not-found')
@PactFolder('src/test/resources/pacts')
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [TestApplication])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SuppressWarnings(['PublicInstanceField', 'NonFinalPublicField', 'JUnitPublicNonTestMethod', 'JUnitPublicField'])
@Slf4j
class SpringRunnerWithBeanThatMustBeClosedProperlyTest {

  @TestTarget
  public Target target = new SpringBootHttpTarget()

  @Autowired
  public TestApplication.ObjectThatMustBeClosed mustBeClosed

  @AfterClass
  static void after() {
    assert TestApplication.ObjectThatMustBeClosed.instance.destroyed
  }

  @State(value = 'book-not-found', action = StateChangeAction.SETUP)
  void booksNoFound() {
    log.debug('state change method called')
    assert !TestApplication.ObjectThatMustBeClosed.instance.destroyed
  }

  @State(value = 'book-not-found', action = StateChangeAction.TEARDOWN)
  void booksNoFoundTeardown() {
    log.debug('state change teardown method called')
    assert !TestApplication.ObjectThatMustBeClosed.instance.destroyed
  }
}
