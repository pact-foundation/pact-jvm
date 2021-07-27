package au.com.dius.pact.provider.spring.target

import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.TargetRequestFilter
import groovy.transform.CompileStatic
import org.junit.runners.model.TestClass
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spock.lang.Specification

@Provider('testProvider')
class MockMvcTargetSpec extends Specification {

  private MockMvcTarget mockMvcTarget

  @RestController
  static class TestController {
    @RequestMapping('/')
    String test() { 'test' }
  }

  @Provider('testProvider')
  @CompileStatic
  static class TestClassWithFilter {
    @TargetRequestFilter
    void requestFilter(MockHttpServletRequestBuilder request) {
      request.header('X-Content-Type', MediaType.APPLICATION_ATOM_XML)
    }
  }

  def setup() {
    mockMvcTarget = new MockMvcTarget()
  }

  def 'only execute the test the configured number of times'() {
    given:
    mockMvcTarget.runTimes = 1
    mockMvcTarget.setTestClass(new TestClass(MockMvcTargetSpec), this)
    def interaction = new RequestResponseInteraction('Test Interaction')
    def controller = Mock(TestController)
    mockMvcTarget.controllers = [ controller ]

    when:
    mockMvcTarget.testInteraction('testConsumer', interaction, UnknownPactSource.INSTANCE, [:], false)

    then:
    1 * controller.test()
  }

  def 'invokes any request filter'() {
    given:
    def testInstance = Spy(TestClassWithFilter)
    mockMvcTarget.setTestClass(new TestClass(TestClassWithFilter), testInstance)
    def interaction = new RequestResponseInteraction('Test Interaction')
    def controller = Mock(TestController)
    mockMvcTarget.controllers = [ controller ]

    when:
    mockMvcTarget.testInteraction('testConsumer', interaction, UnknownPactSource.INSTANCE, [:], false)

    then:
    1 * testInstance.requestFilter(_)
  }
}
