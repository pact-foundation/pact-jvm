package au.com.dius.pact.provider.groovysupport

import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.PactReaderKt
import au.com.dius.pact.model.Request
import au.com.dius.pact.provider.IHttpClientFactory
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import org.apache.http.Header
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.CloseableHttpClient
import org.junit.Before
import org.junit.Test
import org.mockito.invocation.InvocationOnMock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ProviderClientTest {

  private ProviderClient client
  private Request request
  private provider
  private mockHttpClient
  private HttpUriRequest args
  private IHttpClientFactory httpClientFactory

  @Before
  void setup() {
    provider = new ProviderInfo(
      protocol: 'http',
      host: 'localhost',
      port: 8080,
      path: '/'
    )
    request = new Request()
    mockHttpClient = mock CloseableHttpClient
    httpClientFactory = [newClient: { provider -> mockHttpClient } ] as IHttpClientFactory
    client = new ProviderClient(provider, httpClientFactory)
    when(mockHttpClient.execute(any())).thenAnswer { InvocationOnMock invocation ->
      args = invocation.arguments.first()
      [
        getStatusLine: { [getStatusCode: { 200 } ] as StatusLine },
        getAllHeaders: { [] as Header[] },
        getEntity: { },
        close: { }
      ] as CloseableHttpResponse
    }
  }

  @Test
  void 'URL decodes the path'() {
    String path = '%2Fpath%2FTEST+PATH%2F2014-14-06+23%3A22%3A21'
    def request = new Request('GET', path, [:], [:], OptionalBody.body(''.bytes))
    client.makeRequest(request)
    assert args.URI.path == '/path/TEST PATH/2014-14-06 23:22:21'
  }

  @Test
  void 'query parameters must NOT be placed in the body for URL encoded FORM POSTs'() {
    def request = new Request('POST', '/', PactReaderKt.queryStringToMap('a=1&b=11&c=Hello World'),
      ['Content-Type': [ContentType.APPLICATION_FORM_URLENCODED.toString()]], OptionalBody.body('A=B'.bytes))
    client.makeRequest(request)
    assert args.URI.query == 'a=1&b=11&c=Hello+World'
    assert args.entity.content.text == 'A=B'
  }

}
