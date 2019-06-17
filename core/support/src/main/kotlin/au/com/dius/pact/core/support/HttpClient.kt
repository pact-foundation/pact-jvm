package au.com.dius.pact.core.support

import mu.KLogging
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import java.net.URI

/**
 * HTTP client support functions
 */
object HttpClient: KLogging() {

  /**
   * Creates a new HTTP client
   */
  fun newHttpClient(options: Any?, uri: URI, defaultHeaderStore: MutableMap<String, String>): CloseableHttpClient {
    val retryStrategy = CustomServiceUnavailableRetryStrategy(5, 3000)
    val builder = HttpClients.custom().useSystemProperties().setServiceUnavailableRetryStrategy(retryStrategy)
    if (options is List<*>) {
      when (val scheme = options.first().toString().toLowerCase()) {
        "basic" -> {
          if (options.size > 2) {
            val credsProvider = BasicCredentialsProvider()
            credsProvider.setCredentials(AuthScope(uri.host, uri.port),
              UsernamePasswordCredentials(options[1].toString(), options[2].toString()))
            builder.setDefaultCredentialsProvider(credsProvider)
          } else {
            logger.warn { "Basic authentication requires a username and password, ignoring." }
          }
        }
        "bearer" -> {
          if (options.size > 1) {
            defaultHeaderStore["Authorization"] = "Bearer " + options[1].toString()
          } else {
            logger.warn { "Bearer token authentication requires a token, ignoring." }
          }
        }
        else -> logger.warn { "HTTP client Only supports basic and bearer token authentication, got '$scheme', ignoring." }
      }
    }

    return builder.build()
  }
}
