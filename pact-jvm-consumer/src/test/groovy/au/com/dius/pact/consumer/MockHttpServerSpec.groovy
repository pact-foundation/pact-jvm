package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.jerseyclient.BeanIn
import au.com.dius.pact.consumer.jerseyclient.BeanOut
import au.com.dius.pact.consumer.jerseyclient.RootResource
import au.com.dius.pact.model.MockProviderConfig
import org.glassfish.jersey.client.proxy.WebResourceFactory
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest

class MockHttpServerSpec extends Specification {

  @Unroll
  def 'calculated charset test - "#contentTypeHeader"'() {

    expect:
    MockHttpServerKt.calculateCharset(headers).name() == expectedCharset

    where:

    contentTypeHeader               | expectedCharset
    null                            | 'UTF-8'
    'null'                          | 'UTF-8'
    ''                              | 'UTF-8'
    'text/plain'                    | 'UTF-8'
    'text/plain; charset'           | 'UTF-8'
    'text/plain; charset='          | 'UTF-8'
    'text/plain;charset=ISO-8859-1' | 'ISO-8859-1'

    headers = ['Content-Type': contentTypeHeader]

  }

  def 'with no content type defaults to UTF-8'() {
    expect:
    MockHttpServerKt.calculateCharset([:]).name() == 'UTF-8'
  }

  def 'ignores case with the header name'() {
    expect:
    MockHttpServerKt.calculateCharset(['content-type': 'text/plain; charset=ISO-8859-1']).name() == 'ISO-8859-1'
  }

  def 'test behaviour with jersey proxy'() {
    given:
    def pact = ConsumerPactBuilder.consumer('Some Consumer')
      .hasPactWith('Some Provider')
      .uponReceiving('a request with an empty body')
      .path('/hello')
      .method('POST')
      .body('{"test": true}', 'application/json')
      .willRespondWith()
      .status(200)
      .body('{"result": true}', 'application/json')
      .toPact()

    MockProviderConfig config = MockProviderConfig.createDefault()

    when:
    PactVerificationResult result = runConsumerTest(pact, config) { MockServer mockServer ->
      Client client = ClientBuilder.newClient()
      WebTarget webTarget = client.target(mockServer.url)
      RootResource rootResource = WebResourceFactory.newResource(RootResource, webTarget)
      def test = rootResource.getTest(new BeanIn(true))
      assert test == new BeanOut(true)
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
  }

}
