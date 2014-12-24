package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Request
import au.com.dius.pact.provider.groovysupport.ProviderClient
import org.apache.http.Header
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.impl.client.CloseableHttpClient
import org.junit.Before
import org.junit.Test
import org.mockito.invocation.InvocationOnMock

import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ProviderClientTest {

  ProviderClient client
  Request request
  ProviderInfo provider
  def mockHttpClient

  @Before
  void setup() {
    provider = new ProviderInfo()
    client = new ProviderClient(request: request, provider: provider)
    mockHttpClient = mock CloseableHttpClient
    client.metaClass.newClient = { mockHttpClient }
  }

  @Test
  void 'URL decodes the path'() {
    String path = '%2Fpath%2FTEST+PATH%2F2014-14-06+23%3A22%3A21'
    client.request = Request.apply('GET', path, '', [:], '', [:])
    def args
    when(mockHttpClient.execute(any())).thenAnswer( { InvocationOnMock invocation ->
      args = invocation.arguments.first()
      [
          getStatusLine: { [getStatusCode: { 200 }] as StatusLine },
          getAllHeaders: { [] as Header[] },
          getEntity: {},
          close: {}
      ] as CloseableHttpResponse
    })
    client.makeRequest()
    assert args.URI.path == '/path/TEST PATH/2014-14-06 23:22:21'
  }

}
