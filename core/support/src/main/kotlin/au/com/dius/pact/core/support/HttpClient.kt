package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser
import au.com.dius.pact.core.support.expressions.ValueResolver
import mu.KLogging
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.CredentialsProvider
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager
import org.apache.hc.client5.http.socket.ConnectionSocketFactory
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.ssl.SSLContexts
import org.apache.hc.core5.util.TimeValue
import java.net.URI

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

  @JvmOverloads
  fun resolveProperties(resolver: ValueResolver, ep: ExpressionParser = ExpressionParser()): Auth {
    return when (this) {
      is BasicAuthentication -> BasicAuthentication(
        ep.parseExpression(this.username, DataType.RAW, resolver).toString(),
        ep.parseExpression(this.password, DataType.RAW, resolver).toString())
      is BearerAuthentication -> BearerAuthentication(ep.parseExpression(this.token, DataType.RAW, resolver).toString())
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
    val builder = HttpClients.custom().useSystemProperties()
      .setRetryStrategy(DefaultHttpRequestRetryStrategy(maxPublishRetries,
        TimeValue.ofMilliseconds(publishRetryInterval.toLong())))

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
    val credsProvider = SystemDefaultCredentialsProvider()
    credsProvider.setCredentials(AuthScope(uri.host, uri.port),
      UsernamePasswordCredentials(username, password.toCharArray()))
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

    val sslcontext = SSLContexts.custom().loadTrustMaterial(TrustSelfSignedStrategy()).build()
    val sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
      .setSslContext(sslcontext).build()
    builder.setConnectionManager(
        BasicHttpClientConnectionManager(
          RegistryBuilder.create<ConnectionSocketFactory>()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", sslSocketFactory)
            .build()
        )
      )
  }
}
