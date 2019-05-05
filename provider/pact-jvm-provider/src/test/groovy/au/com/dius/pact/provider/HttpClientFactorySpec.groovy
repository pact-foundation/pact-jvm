package au.com.dius.pact.provider

import org.apache.http.impl.client.CloseableHttpClient
import spock.lang.Specification

class HttpClientFactorySpec extends Specification {

  def 'creates a new client by default'() {
    expect:
    new HttpClientFactory().newClient(new ProviderInfo()) != null
  }

  def 'if createClient is provided as a closure, invokes that'() {
    given:
    def provider = new ProviderInfo()
    def httpClient = Mock(CloseableHttpClient)
    provider.createClient = { httpClient }

    expect:
    new HttpClientFactory().newClient(provider) == httpClient
  }

  def 'if createClient is provided as a string, invokes t`hat as Groovy code'() {
    given:
    def provider = new ProviderInfo()
    provider.createClient = '[:] as org.apache.http.impl.client.CloseableHttpClient'

    expect:
    new HttpClientFactory().newClient(provider) != null
  }

}
