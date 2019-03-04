package au.com.dius.pact.pactbroker

import org.apache.http.HttpResponse
import org.apache.http.annotation.Contract
import org.apache.http.annotation.ThreadingBehavior
import org.apache.http.client.ServiceUnavailableRetryStrategy
import org.apache.http.protocol.HttpContext
import org.apache.http.util.Args

/**
 * Default implementation of the [ServiceUnavailableRetryStrategy] interface.
 * that retries `503` (Service Unavailable) responses for a fixed number of times
 * at a fixed interval.
 *
 * @since 4.2
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
class CustomServiceUnavailableRetryStrategy(
  /**
   * Maximum number of allowed retries if the server responds with a HTTP code
   * in our retry code list. Default value is 1.
   */
  private val maxRetries: Int,
  retryInterval: Int
) : ServiceUnavailableRetryStrategy {

  /**
   * Retry interval between subsequent requests, in milliseconds. Default
   * value is 1 second.
   */
  private val retryInterval: Long

  init {
    Args.positive(maxRetries, "Max retries")
    Args.positive(retryInterval, "Retry interval")
    this.retryInterval = retryInterval.toLong()
  }

  override fun retryRequest(response: HttpResponse, executionCount: Int, context: HttpContext): Boolean {
    return executionCount <= maxRetries && response.statusLine.statusCode >= 500
  }

  override fun getRetryInterval(): Long {
    return retryInterval
  }
}
