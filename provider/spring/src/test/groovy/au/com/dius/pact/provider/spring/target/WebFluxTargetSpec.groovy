package au.com.dius.pact.provider.spring.target

import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.TargetRequestFilter
import org.junit.runners.model.TestClass
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import spock.lang.Specification

import static org.springframework.web.reactive.function.server.RequestPredicates.GET

@Provider('testProvider')
class WebFluxTargetSpec extends Specification {

  static final TEST_HEADER_NAME = 'X-Content-Type'
  static final TEST_MEDIA_TYPE = MediaType.APPLICATION_ATOM_XML.toString()

  class TestHandler {
    Mono<ServerResponse> test(ServerRequest ignore) {
      ServerResponse.ok().build()
    }

    Mono<ServerResponse> testXContentType(ServerRequest serverRequest) {
      def xContentTypeValues = serverRequest.headers().header(WebFluxTargetSpec.TEST_HEADER_NAME)

      assert !xContentTypeValues.empty
      assert WebFluxTargetSpec.TEST_MEDIA_TYPE == xContentTypeValues.first()

      ServerResponse.ok().build()
    }
  }

  @RestController
  class TestController {

    @GetMapping('/')
    String test() {
      'OK'
    }

  }

  @Provider('testProvider')
  class TestClassWithFilter {
    @TargetRequestFilter
    void requestFilter(WebTestClient.RequestHeadersSpec<?> request) {
      request.header(WebFluxTargetSpec.TEST_HEADER_NAME, WebFluxTargetSpec.TEST_MEDIA_TYPE)
    }
  }

  def 'execute the test against router function'() {
    given:
    def target = new WebFluxTarget()
    target.setTestClass(new TestClass(WebFluxTargetSpec), this)
    def interaction = new RequestResponseInteraction('Test Interaction')
    def handler = Spy(TestHandler)
    target.routerFunction = RouterFunctions.route(GET('/'), handler.&test)

    when:
    target.testInteraction('testConsumer', interaction, UnknownPactSource.INSTANCE, [:], false)

    then:
    1 * handler.test(_)
  }

  def 'execute the test against controller'() {
    given:
    def target = new WebFluxTarget()
    target.setTestClass(new TestClass(WebFluxTargetSpec), this)
    def interaction = new RequestResponseInteraction('Test Interaction')
    def controller = Spy(TestController)
    target.controllers = [controller]

    when:
    target.testInteraction('testConsumer', interaction, UnknownPactSource.INSTANCE, [:], false)

    then:
    1 * controller.test()
  }

  def 'invokes any request filter'() {
    given:
    def target = new WebFluxTarget()
    def testInstance = Spy(TestClassWithFilter)
    target.setTestClass(new TestClass(TestClassWithFilter), testInstance)
    def interaction = new RequestResponseInteraction('Test Interaction')
    def handler = Spy(TestHandler)
    target.routerFunction = RouterFunctions.route(GET('/'), handler.&testXContentType)

    when:
    target.testInteraction('testConsumer', interaction, UnknownPactSource.INSTANCE, [:], false)

    then:
    1 * testInstance.requestFilter(_)
  }
}
