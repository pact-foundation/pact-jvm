package au.com.dius.pact.consumer.junit5.xml

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'XMLProvider', pactVersion = PactSpecVersion.V3)
class XMLContentTypePactTest {
  def example = '<?xml version=\"1.0\" encoding=\"utf-8\"?><example>foo</example>'

  @Pact(consumer = 'XMLConsumer2')
  RequestResponsePact xmlMessage(PactDslWithProvider builder) {
    builder
      .uponReceiving('a POST request with an XML message')
      .method('POST')
      .path('/message')
      .bodyMatchingContentType('application/xml', example)
      .willRespondWith()
      .status(200)
      .bodyMatchingContentType('application/xml', example)
      .toPact()
  }

  @Test

  void testXMLPost(MockServer mockServer) {
    HttpResponse httpResponse = Request.post("${mockServer.url}/message")
      .bodyString(
        '''<?xml version="1.0" encoding="UTF-8"?>
        <Message type="Request">
          <Head>
            <Client name="WebCheck">
              <Version>2.2.8.3</Version>
            </Client>
            <Server>
              <Name>SrvCheck</Name>
              <Version>3.0</Version>
            </Server>
            <Authentication>
              <User>peter</User>
              <Password>token_placeholder</Password>
            </Authentication>
            <Token>1234567323211242144</Token>
          </Head>
          <Body>
            <Call method="getInfo" service="CheckRpcService">
              <Param name="exportId">
                <ExportId>123456789</ExportId>
              </Param>
            </Call>
          </Body>
        </Message>
        ''', ContentType.APPLICATION_XML
      )
      .execute().returnResponse()
    assert httpResponse.code == 200
  }
}
