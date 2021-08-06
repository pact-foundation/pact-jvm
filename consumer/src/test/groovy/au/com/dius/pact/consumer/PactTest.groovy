package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockHttpsProviderConfig
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import groovy.json.JsonSlurper
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.ssl.SSLContexts
import org.junit.jupiter.api.Test

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest
import static io.ktor.network.tls.certificates.CertificatesKt.generateCertificate
import static org.hamcrest.CoreMatchers.instanceOf
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertEquals

@SuppressWarnings('ThrowRuntimeException')
class PactTest {

  @Test
  void testPact() {
    RequestResponsePact pact = ConsumerPactBuilder
      .consumer('Some Consumer')
      .hasPactWith('Some Provider')
      .uponReceiving('a request to say Hello')
        .path('/hello')
        .method('POST')
        .body('{"name": "harry"}')
      .willRespondWith()
        .status(200)
        .body('{"hello": "harry"}', ContentType.APPLICATION_JSON.toString())
      .toPact()

    MockProviderConfig config = MockProviderConfig.createDefault()
    PactVerificationResult result = runConsumerTest(pact, config, new PactTestRun<Boolean>() {
      @Override
      Boolean run(MockServer mockServer, PactTestExecutionContext context) throws IOException {
        Map expectedResponse = [hello: 'harry']
        assertEquals(expectedResponse, new ConsumerClient(mockServer.url).post('/hello',
          '{"name": "harry"}', ContentType.APPLICATION_JSON))
        true
      }
    })

    if (result instanceof PactVerificationResult.Error) {
      throw new RuntimeException(((PactVerificationResult.Error) result).error)
    }

    assertThat(result, is(instanceOf(PactVerificationResult.Ok)))
  }

  @Test
  void testPactHttps() {
    RequestResponsePact pact = ConsumerPactBuilder
      .consumer('Some Consumer')
      .hasPactWith('Some Provider')
      .uponReceiving('a request to say Hello')
      .path('/hello')
      .method('POST')
      .body('{"name": "harry"}')
      .willRespondWith()
      .status(200)
      .body('{"hello": "harry"}')
      .toPact()

    def jksFile = File.createTempFile('PactTest', '.jks')
    def keystore = generateCertificate(jksFile, 'SHA1withRSA', 'PactTest', 'changeit', 'changeit', 1024)

    MockProviderConfig config = new MockHttpsProviderConfig('127.0.0.1', 8443, PactSpecVersion.V3,
      keystore, 'PactTest', 'changeit', 'changeit')
    PactVerificationResult result = runConsumerTest(pact, config, new PactTestRun<Boolean>() {
      @Override
      Boolean run(MockServer mockServer, PactTestExecutionContext context) throws IOException {
        assert mockServer.url.startsWith('https://')
        Map expectedResponse = [hello: 'harry']

        def sslcontext = SSLContexts.custom().loadTrustMaterial(new TrustSelfSignedStrategy()).build()
        def sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
          .setSslContext(sslcontext).build()
        def httpclient = HttpClientBuilder.create()
          .setConnectionManager(new BasicHttpClientConnectionManager(
            RegistryBuilder.create()
              .register("http", PlainConnectionSocketFactory.getSocketFactory())
              .register("https", sslSocketFactory)
              .build()
          ))
          .build()

        def post = new HttpPost(mockServer.url + '/hello')
        post.setEntity(new StringEntity('{"name": "harry"}', ContentType.APPLICATION_JSON))
        def response = null
        def actualResponse = null
        try {
          response = httpclient.execute(post)
          if (response.code == 200) {
            actualResponse = new JsonSlurper().parseText(EntityUtils.toString(response.entity))
          }
        } finally {
          response?.close()
        }

        assertEquals(expectedResponse, actualResponse)
        true
      }
    })

    if (result instanceof PactVerificationResult.Error) {
      throw new RuntimeException(((PactVerificationResult.Error) result).error)
    }

    assertThat(result, is(instanceOf(PactVerificationResult.Ok)))
  }
}
