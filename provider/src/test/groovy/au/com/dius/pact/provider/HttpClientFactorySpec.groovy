package au.com.dius.pact.provider

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.RedirectExec
import org.apache.hc.client5.http.protocol.RedirectStrategy
import spock.lang.Issue
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

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

  def 'if createClient is provided as a string, invokes that as Groovy code'() {
    given:
    def provider = new ProviderInfo()
    provider.createClient = '[:] as org.apache.hc.client5.http.impl.classic.CloseableHttpClient'

    expect:
    new HttpClientFactory().newClient(provider) != null
  }

  @Issue('#1323')
  @RestoreSystemProperties
  def 'if pact.verifier.enableRedirectHandling is set, does not disable redirect handler'() {
    given:
    def provider = new ProviderInfo()
    System.setProperty('pact.verifier.enableRedirectHandling', 'true')

    when:
    def client = new HttpClientFactory().newClient(provider)

    then:
    client.execChain.handler instanceof RedirectExec
    client.execChain.handler.redirectStrategy instanceof RedirectStrategy
  }

  @Issue('#1323')
  @RestoreSystemProperties
  def 'if pact.verifier.enableRedirectHandling is not set to true, disable the redirect handler'() {
    given:
    def provider = new ProviderInfo()
    System.setProperty('pact.verifier.enableRedirectHandling', 'false')

    when:
    def client = new HttpClientFactory().newClient(provider)

    then:
    !(client.execChain.handler instanceof RedirectExec)
  }

  @Issue('#1323')
  def 'if pact.verifier.enableRedirectHandling is not set, disable the redirect handler'() {
    given:
    def provider = new ProviderInfo()

    when:
    def client = new HttpClientFactory().newClient(provider)

    then:
    !(client.execChain.handler instanceof RedirectExec)
  }
}
