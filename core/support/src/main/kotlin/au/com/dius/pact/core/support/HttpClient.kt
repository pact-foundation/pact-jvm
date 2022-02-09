package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser.parseExpression
import au.com.dius.pact.core.support.expressions.ValueResolver
import mu.KLogging
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.SystemDefaultCredentialsProvider
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.BasicHeader
import org.apache.http.ssl.SSLContextBuilder
import java.net.URI
import java.security.cert.X509Certificate

/**
 * Authentication options
 */
sealed class Auth {
  /**
   * Basic authentication (username/password)
   */
  data class BasicAuthentication(val username: String, val password: String) : Auth()

  /**
   * Bearer token authentication
   */
  data class BearerAuthentication(val token: String) : Auth()

  fun resolveProperties(resolver: ValueResolver): Auth {
    return when (this) {
      is BasicAuthentication -> BasicAuthentication(parseExpression(this.username, DataType.RAW, resolver).toString(),
          parseExpression(this.password, DataType.RAW, resolver).toString())
      is BearerAuthentication -> BearerAuthentication(parseExpression(this.token, DataType.RAW, resolver).toString())
    }
  }

  fun legacyForm(): List<String> {
    return when (this) {
      is BasicAuthentication -> listOf("basic", this.username, this.password)
      is BearerAuthentication -> listOf("bearer", this.token)
    }
  }
}

/**
 * HTTP client support functions
 */
object HttpClient : KLogging() {

  /**
   * Creates a new HTTP client
   */
  fun newHttpClient(
    authentication: Any?,
    uri: URI,
    maxPublishRetries: Int = 5,
    publishRetryInterval: Int = 3000,
    insecureTLS: Boolean = false
  ): Pair<CloseableHttpClient, CredentialsProvider?> {
    val retryStrategy = CustomServiceUnavailableRetryStrategy(maxPublishRetries, publishRetryInterval)
    val builder = HttpClients.custom().useSystemProperties().setServiceUnavailableRetryStrategy(retryStrategy)

    val defaultHeaders = mutableMapOf<String, String>()
    val credsProvider = when (authentication) {
      is Auth -> {
        when (authentication) {
          is Auth.BasicAuthentication -> basicAuth(uri, authentication.username, authentication.password, builder)
          is Auth.BearerAuthentication -> {
            defaultHeaders["Authorization"] = "Bearer " + authentication.token
            SystemDefaultCredentialsProvider()
          }
        }
      }
      is List<*> -> {
        when (val scheme = authentication.first().toString().toLowerCase()) {
          "basic" -> {
            if (authentication.size > 2) {
              basicAuth(uri, authentication[1].toString(), authentication[2].toString(), builder)
            } else {
              logger.warn { "Basic authentication requires a username and password, ignoring." }
              SystemDefaultCredentialsProvider()
            }
          }
          "bearer" -> {
            if (authentication.size > 1) {
              defaultHeaders["Authorization"] = "Bearer " + authentication[1].toString()
            } else {
              logger.warn { "Bearer token authentication requires a token, ignoring." }
            }
            SystemDefaultCredentialsProvider()
          }
          else -> {
            logger.warn { "HTTP client Only supports basic and bearer token authentication, got '$scheme', ignoring." }
            SystemDefaultCredentialsProvider()
          }
        }
      }
      else -> SystemDefaultCredentialsProvider()
    }

    builder.setDefaultHeaders(defaultHeaders.map { BasicHeader(it.key, it.value) })

    if (insecureTLS) {
      setupInsecureTLS(builder)
    }

    return builder.build() to credsProvider
  }

  private fun basicAuth(
    uri: URI,
    username: String,
    password: String,
    builder: HttpClientBuilder
  ): CredentialsProvider {
    val credsProvider = BasicCredentialsProvider()
    credsProvider.setCredentials(AuthScope(uri.host, uri.port),
      UsernamePasswordCredentials(username, password))
    builder.setDefaultCredentialsProvider(credsProvider)
    return credsProvider
  }

  private fun setupInsecureTLS(builder: HttpClientBuilder) {
    logger.warn {
      """
      *****************************************************************
                           Setting Insecure TLS
      This will disable hostname validation and trust all certificates!               
      *****************************************************************
      """
    }

    val trustStrategy = TrustStrategy { _: Array<X509Certificate>, _: String -> true }
    val sslContext = SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build()
    builder.setSSLContext(sslContext)
    val hostnameVerifier = NoopHostnameVerifier()
    val sslSocketFactory = SSLConnectionSocketFactory(sslContext, hostnameVerifier)
    val socketFactoryRegistry = RegistryBuilder.create<ConnectionSocketFactory>()
      .register("http", PlainConnectionSocketFactory.getSocketFactory())
      .register("https", sslSocketFactory)
      .build()
    val connMgr = PoolingHttpClientConnectionManager(socketFactoryRegistry)
    builder.setConnectionManager(connMgr)
  }
}
