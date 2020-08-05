package au.com.dius.pact.consumer.junit5.xml

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.consumer.xml.PactXmlBuilder
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.http.HttpResponse
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static au.com.dius.pact.consumer.dsl.Matchers.regexp

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'XMLProvider')
class XMLPactTest {
  @Pact(consumer = 'XMLConsumer')
  RequestResponsePact xmlMessage(PactDslWithProvider builder) {
    builder
      .uponReceiving('a POST request with an XML message')
      .method('POST')
      .path('/message')
      .body(new PactXmlBuilder('Message').build(message -> {
        message.setAttributes([type: 'Request'])
        message.appendElement('Head', [:], head -> {
          head.appendElement('Client', [name: 'WebCheck'], client -> {
            client.appendElement('Version', regexp(/\d+\.\d+\.\d+\.\d+/, "2.2.8.2"))
          })
          head.appendElement('Server', [:], server -> {
            server.appendElement('Name', [:], "SrvCheck")
            server.appendElement('Version', [:], "3.0")
          })
          head.appendElement('Authentication', [:], authentication -> {
            authentication.appendElement('User', [:], regexp(/\w+/, "user_name"))
            authentication.appendElement('Password', [:], regexp(/\w+/, "password"))
          })
          head.appendElement('Token', [:], '1234567323211242144')
        })
        message.appendElement('Body', [:], body -> {
          body.appendElement('Call', [method: 'getInfo', service: 'CheckRpcService'], call -> {
            call.appendElement('Param', [name: regexp(/exportId|mtpId/, 'exportId')], param -> {
              param.appendElement('ExportId', regexp(/\d+/, '1234567890'))
            })
          })
        })
      }))
      .willRespondWith()
      .status(200)
      .body(new PactXmlBuilder("Message").build(message -> {
        message.appendElement('Head', [:], head -> {
          head.appendElement('Server', [:], server -> {
            server.appendElement('Name', [:], regexp(/\w+/, 'server_name'))
            server.appendElement('Version', [:], regexp(/.+/, 'server_version'))
            server.appendElement('Timestamp', [:], regexp(/.+/, 'server_timestamp'))
          })
        })
        message.appendElement('Body', [:], body -> {
          body.appendElement('Result', [state: 'SUCCESS'])
        })
      }))
      .toPact()
  }

  @Test
  void testXMLPost(MockServer mockServer) {
    HttpResponse httpResponse = Request.Post("${mockServer.url}/message")
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
    assert httpResponse.statusLine.statusCode == 200
  }
}
