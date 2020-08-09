package au.com.dius.pact.provider.spring

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.provider.ProviderInfo
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import spock.lang.Specification

import static org.springframework.web.reactive.function.server.RequestPredicates.GET
import static org.springframework.web.reactive.function.server.RequestPredicates.POST

class WebFluxProviderVerifierSpec extends Specification {

  def verifier = new WebFluxProviderVerifier()

  class TestHandler {
    Mono<ServerResponse> testNoBody(ServerRequest ignore) {
      ServerResponse.ok().build()
    }

    Mono<ServerResponse> testBody(ServerRequest serverRequest) {
      ServerResponse.ok().body(serverRequest.bodyToMono(String), String)
    }

    Mono<ServerResponse> testMultiBody(ServerRequest serverRequest) {
      ServerResponse.ok().body(serverRequest.multipartData()
        .map { it.getFirst('file') }
        .cast(FilePart)
        .zipWhen { it.content().next() }
        .map {
          def part = it.first()
          def buffer = it.last()

          [part.name(), part.filename(), part.headers()['Content-Type'].first(), toString(buffer)].join('|')
        }, String)
    }
  }

  def 'executing a request against web test client with a body'() {
    given:
    def body = '"This is a body"'
    def request = new Request(method: 'POST', body: OptionalBody.body(body.bytes))
    def handler = new TestHandler()
    def routerFunction = RouterFunctions.route(POST('/'), handler.&testBody)
    def webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build()

    when:
    def exchangeResult = verifier.executeWebFluxRequest(webTestClient, request, new ProviderInfo())

    then:
    exchangeResult.responseHeaders['Content-Type'].first() == 'text/plain;charset=UTF-8'
    new String(exchangeResult.responseBody) == body
  }

  def 'executing a request against web test client with no body'() {
    given:
    def request = new Request()
    def handler = new TestHandler()
    def routerFunction = RouterFunctions.route(GET('/'), handler.&testNoBody)
    def webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build()

    when:
    def exchangeResult = verifier.executeWebFluxRequest(webTestClient, request, new ProviderInfo())

    then:
    exchangeResult.responseHeaders['Content-Type'] == null
    exchangeResult.responseBody == null
  }

  def 'executing a request against web test client with a multipart file upload'() {
    given:
    def request = new Request(method: 'POST').withMultipartFileUpload('file', 'filename', 'text/plain', 'file,contents')
    def handler = new TestHandler()
    def routerFunction = RouterFunctions.route(POST('/'), handler.&testMultiBody)
    def webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build()

    when:
    def exchangeResult = verifier.executeWebFluxRequest(webTestClient, request, new ProviderInfo())

    then:
    exchangeResult.responseHeaders['Content-Type'].first() == 'text/plain;charset=UTF-8'
    new String(exchangeResult.responseBody) == 'file|filename|text/plain|file,contents'
  }

  private toString(DataBuffer buffer) {
    byte[] bytes = new byte[buffer.readableByteCount()]
    buffer.read(bytes)
    DataBufferUtils.release(buffer)
    new String(bytes)
  }

}
