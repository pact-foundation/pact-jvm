package au.com.dius.pact.provider

import groovy.lang.Binding
import groovy.lang.Closure
import groovy.lang.GroovyShell
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.AllowAllHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.ssl.SSLContextBuilder
import org.apache.http.ssl.TrustStrategy
import java.security.cert.X509Certificate

/**
 * HTTP Client Factory
 */
class HttpClientFactory: IHttpClientFactory {

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
            HttpClients.createDefault()
        }
    }

    private fun createWithTrustStore(provider: IProviderInfo): CloseableHttpClient {
        val password = provider.trustStorePassword.orEmpty().toCharArray()
        return HttpClients
            .custom()
            .setSslcontext(SSLContextBuilder().loadTrustMaterial(provider.trustStore, password).build())
            .build()
    }

    private fun createInsecure(): CloseableHttpClient {
        val b = HttpClientBuilder.create()

        // setup a Trust Strategy that allows all certificates.
        //
        val trustStratergy = TrustStrategy { _: Array<X509Certificate>, _: String -> true }
        val sslContext = SSLContextBuilder().loadTrustMaterial(null, trustStratergy).build()
        b.setSslcontext(sslContext)
        // don't check Hostnames, either.
        //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
        val hostnameVerifier = AllowAllHostnameVerifier()

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
