package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Request
import groovyx.net.http.RESTClient
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
    mockHttpClient = mock RESTClient
    client.metaClass.newClient = { mockHttpClient }
  }

  @Test
  void 'URL decodes the path'() {
    String path = '%2Fpath%2FTEST+PATH%2F2014-14-06+23%3A22%3A21'
    client.request = Request.apply('GET', path, '', [:], '', [:])
    def args
    when(mockHttpClient.get(any())).thenAnswer( { InvocationOnMock invocation ->
      args = invocation.arguments.first()
      null
    })
    client.makeRequest()
    assert args.path == '/path/TEST PATH/2014-14-06 23:22:21'
  }

}
