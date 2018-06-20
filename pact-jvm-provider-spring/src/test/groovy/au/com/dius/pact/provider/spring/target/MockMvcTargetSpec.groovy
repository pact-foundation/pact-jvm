package au.com.dius.pact.provider.spring.target

import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.provider.junit.Provider
import org.junit.runners.model.TestClass
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spock.lang.Specification

@Provider('testProvider')
class MockMvcTargetSpec extends Specification {

  private MockMvcTarget mockMvcTarget

  @RestController
  class TestController {
    @RequestMapping('/')
    String test() { 'test' }
  }

  def setup() {
    mockMvcTarget = new MockMvcTarget()
  }

  def 'only execute the test the configured number of times'() {
    given:
    mockMvcTarget.runTimes = 1
    mockMvcTarget.setTestClass(new TestClass(MockMvcTargetSpec), this)
    def interaction = new RequestResponseInteraction(description: 'Test Interaction', request: new Request(),
      response: new Response())
    def controller = Mock(TestController)
    mockMvcTarget.controllers = [ controller ]

    when:
    mockMvcTarget.testInteraction('testConsumer', interaction, UnknownPactSource.INSTANCE)

    then:
    1 * controller.test()
  }

}
