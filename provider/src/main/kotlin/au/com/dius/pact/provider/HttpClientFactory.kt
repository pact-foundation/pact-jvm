package au.com.dius.pact.provider

import groovy.lang.Binding
import groovy.lang.Closure
import groovy.lang.GroovyShell
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.socket.ConnectionSocketFactory
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.client5.http.ssl.TrustAllStrategy
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.apache.hc.core5.ssl.SSLContexts

/**
 * HTTP Client Factory
 */
class HttpClientFactory : IHttpClientFactory {

  override fun newClient(provider: IProviderInfo): CloseableHttpClient {
    return if (provider.createClient != null) {
      if (provider.createClient is Closure<*>) {
        (provider.createClient as Closure<*>).call(provider) as CloseableHttpClient
      } else {
        val binding = Binding()
        binding.setVariable("provider", provider)
        val shell = GroovyShell(binding)
        shell.evaluate(provider.createClient.toString()) as CloseableHttpClient
      }
    } else if (provider.insecure) {
      createInsecure()
    } else if (provider.trustStore != null && provider.trustStorePassword != null) {
      createWithTrustStore(provider)
    } else {
      val builder = HttpClients.custom().useSystemProperties()
      val enableRedirectHandling = System.getProperty("pact.verifier.enableRedirectHandling")
      if (enableRedirectHandling.isNullOrEmpty() || enableRedirectHandling != "true") {
        builder.disableRedirectHandling()
      }
      builder.build()
    }
  }

  private fun createWithTrustStore(provider: IProviderInfo): CloseableHttpClient {
    val password = provider.trustStorePassword.orEmpty().toCharArray()
    val sslcontext = SSLContexts.custom().loadTrustMaterial(provider.trustStore, password).build()
    val socketFactoryRegistry = RegistryBuilder.create<ConnectionSocketFactory>()
      .register("http", PlainConnectionSocketFactory.INSTANCE)
      .register("https", SSLConnectionSocketFactory(sslcontext))
      .build()
    val connManager = PoolingHttpClientConnectionManager(socketFactoryRegistry)
    val builder = HttpClients
      .custom()
      .useSystemProperties()
      .setConnectionManager(connManager);
    val enableRedirectHandling = System.getProperty("pact.verifier.enableRedirectHandling")
    if (enableRedirectHandling.isNullOrEmpty() || enableRedirectHandling != "true") {
      builder.disableRedirectHandling()
    }
    return builder.build()
  }

  private fun createInsecure(): CloseableHttpClient {
    val b = HttpClientBuilder.create().useSystemProperties()
    val enableRedirectHandling = System.getProperty("pact.verifier.enableRedirectHandling")
    if (enableRedirectHandling.isNullOrEmpty() || enableRedirectHandling != "true") {
      b.disableRedirectHandling()
    }

    // setup a Trust Strategy that allows all certificates.
    //
    val sslContext = SSLContextBuilder().loadTrustMaterial(TrustAllStrategy()).build()
    // don't check Hostnames, either.
    //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
    val hostnameVerifier = NoopHostnameVerifier()

    // here's the special part:
    //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
    //      -- and create a Registry, to register it.
    //
    val sslSocketFactory = SSLConnectionSocketFactory(sslContext, hostnameVerifier)
    val socketFactoryRegistry = RegistryBuilder.create<ConnectionSocketFactory>()
        .register("http", PlainConnectionSocketFactory.getSocketFactory())
        .register("https", sslSocketFactory)
        .build()

    // now, we create connection-manager using our Registry.
    //      -- allows multi-threaded use
    val connMgr = PoolingHttpClientConnectionManager(socketFactoryRegistry)
    b.setConnectionManager(connMgr)

    // finally, build the HttpClient;
    //      -- done!
    return b.build()
  }
}
