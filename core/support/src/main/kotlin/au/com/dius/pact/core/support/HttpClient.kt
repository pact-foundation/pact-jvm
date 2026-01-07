package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.Auth.Companion.DEFAULT_AUTH_HEADER
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser
import au.com.dius.pact.core.support.expressions.ValueResolver
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.CredentialsProvider
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.TlsSocketStrategy
import org.apache.hc.client5.http.ssl.TrustAllStrategy
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.ssl.SSLContexts
import org.apache.hc.core5.util.TimeValue
import java.net.URI
import java.util.Locale

private val logger = KotlinLogging.logger {}

/**
 * Authentication options
 */
sealed class Auth {
  /**
   * No Auth
   */
  object None : Auth()

  /**
   * Basic authentication (username/password)
   */
  data class BasicAuthentication(val username: String, val password: String) : Auth()

  /**
   * Bearer token authentication
   */
  data class BearerAuthentication(val token: String, val headerName: String) : Auth()

  @JvmOverloads
  fun resolveProperties(resolver: ValueResolver, ep: ExpressionParser = ExpressionParser()): Auth {
    return when (this) {
      is BasicAuthentication -> BasicAuthentication(
        ep.parseExpression(this.username, DataType.RAW, resolver).toString(),
        ep.parseExpression(this.password, DataType.RAW, resolver).toString())
      is BearerAuthentication -> BearerAuthentication(
        ep.parseExpression(this.token, DataType.RAW, resolver).toString(),
        ep.parseExpression(this.headerName, DataType.RAW, resolver).toString()
      )
      else -> this
    }
  }

  fun legacyForm(): List<String> {
    return when (this) {
      is BasicAuthentication -> listOf("basic", this.username, this.password)
      is BearerAuthentication -> listOf("bearer", this.token)
      else -> emptyList()
    }
  }

  companion object {
    const val DEFAULT_AUTH_HEADER = "Authorization"
  }
}

private class RetryAnyMethod(
  maxRetries: Int,
  defaultRetryInterval: TimeValue
): DefaultHttpRequestRetryStrategy(maxRetries, defaultRetryInterval) {
  override fun handleAsIdempotent(request: HttpRequest) = true
}

/**
 * HTTP client support functions
 */
object HttpClient {

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
      .setRetryStrategy(RetryAnyMethod(maxPublishRetries,
        TimeValue.ofMilliseconds(publishRetryInterval.toLong())))

    val defaultHeaders = mutableMapOf<String, String>()
    val credsProvider = when (authentication) {
      is Auth -> {
        when (authentication) {
          is Auth.BasicAuthentication -> basicAuth(uri, authentication.username, authentication.password, builder)
          is Auth.BearerAuthentication -> {
            defaultHeaders[authentication.headerName] = "Bearer " + authentication.token
            SystemDefaultCredentialsProvider()
          }
          else -> SystemDefaultCredentialsProvider()
        }
      }
      is List<*> -> {
        logger.warn {
          "You are using a deprecated form of authentication as an unstructured list. This has been replaced with the" +
            " the au.com.dius.pact.core.support.Auth class."
        }
        when (val scheme = authentication.first().toString().lowercase(Locale.getDefault())) {
          "basic" -> {
            if (authentication.size > 2) {
              basicAuth(uri, authentication[1].toString(), authentication[2].toString(), builder)
            } else {
              logger.warn { "Basic authentication requires a username and password, ignoring." }
              SystemDefaultCredentialsProvider()
            }
          }
          "bearer" -> {
            if (authentication.size > 2) {
              defaultHeaders[authentication[2].toString()] = "Bearer " + authentication[1].toString()
            } else if (authentication.size > 1) {
              defaultHeaders[DEFAULT_AUTH_HEADER] = "Bearer " + authentication[1].toString()
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

    val sslContext = SSLContexts.custom().loadTrustMaterial(TrustAllStrategy()).build()
    val registry = RegistryBuilder.create<TlsSocketStrategy>()
      .register("https", DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE))
    val connectionManager = BasicHttpClientConnectionManager.create(registry.build())
    builder.setConnectionManager(connectionManager)
  }
}
