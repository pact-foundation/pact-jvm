package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.DirectorySource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactBrokerSource
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.HttpClientFactory
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import org.apache.http.client.methods.HttpUriRequest
import java.net.URL
import java.net.URLClassLoader
import java.util.function.Function
import java.util.function.Supplier

/**
 * Interface to a test target
 */
interface TestTarget {
  /**
   * Returns information about the provider
   */
  fun getProviderInfo(serviceName: String, pactSource: PactSource? = null): ProviderInfo

  /**
   * Prepares the request for the interaction.
   *
   * @return a pair of the client class and request to use for the test, or null if there is none
   */
  fun prepareRequest(interaction: Interaction, context: Map<String, Any>): Pair<Any, Any>?

  /**
   * If this is a request response (HTTP or HTTPS) target
   */
  fun isHttpTarget(): Boolean

  /**
   * Executes the test (using the client and request from prepareRequest, if any)
   *
   * @return Map of failures, or an empty map if there were not any
   */
  fun executeInteraction(client: Any?, request: Any?): Map<String, Any>

  /**
   * Prepares the verifier for use during the test
   */
  fun prepareVerifier(verifier: IProviderVerifier, testInstance: Any)
}

/**
 * Test target for HTTP tests. This is the default target.
 *
 * @property host Host to bind to. Defaults to localhost.
 * @property port Port that the provider is running on. Defaults to 8080.
 * @property path The path that the provider is mounted on. Defaults to the root path.
 */
open class HttpTestTarget @JvmOverloads constructor (
  val host: String = "localhost",
  val port: Int = 8080,
  val path: String = "/"
) : TestTarget {
  override fun isHttpTarget() = true

  override fun getProviderInfo(serviceName: String, pactSource: PactSource?): ProviderInfo {
    val providerInfo = ProviderInfo(serviceName)
    providerInfo.port = port
    providerInfo.host = host
    providerInfo.protocol = "http"
    providerInfo.path = path
    return providerInfo
  }

  override fun prepareRequest(interaction: Interaction, context: Map<String, Any>): Pair<Any, Any>? {
    val providerClient = ProviderClient(getProviderInfo("provider"), HttpClientFactory())
    if (interaction is RequestResponseInteraction) {
      return providerClient.prepareRequest(interaction.request.generatedRequest(context)) to providerClient
    }
    throw UnsupportedOperationException("Only request/response interactions can be used with an HTTP test target")
  }

  override fun prepareVerifier(verifier: IProviderVerifier, testInstance: Any) { }

  override fun executeInteraction(client: Any?, request: Any?): Map<String, Any> {
    val providerClient = client as ProviderClient
    val httpRequest = request as HttpUriRequest
    return providerClient.executeRequest(providerClient.getHttpClient(), httpRequest)
  }

  companion object {
    /**
     * Creates a HttpTestTarget from a URL. If the URL does not contain a port, 8080 will be used.
     */
    @JvmStatic
    fun fromUrl(url: URL) = HttpTestTarget(url.host,
        if (url.port == -1) 8080 else url.port,
        if (url.path == null) "/" else url.path)
  }
}

/**
 * Test target for providers using HTTPS.
 *
 * @property host Host to bind to. Defaults to localhost.
 * @property port Port that the provider is running on. Defaults to 8080.
 * @property path The path that the provider is mounted on. Defaults to the root path.
 * @property insecure Supports using certs that will not be verified. You need this enabled if you are using self-signed
 * or untrusted certificates. Defaults to false.
 */
open class HttpsTestTarget @JvmOverloads constructor (
  host: String = "localhost",
  port: Int = 8443,
  path: String = "",
  val insecure: Boolean = false
) : HttpTestTarget(host, port, path) {

  override fun getProviderInfo(serviceName: String, pactSource: PactSource?): ProviderInfo {
    val providerInfo = super.getProviderInfo(serviceName, pactSource)
    providerInfo.protocol = "https"
    providerInfo.insecure = insecure
    return providerInfo
  }

  companion object {
    /**
     * Creates a HttpsTestTarget from a URL. If the URL does not contain a port, 443 will be used.
     *
     * @param insecure Supports using certs that will not be verified. You need this enabled if you are using self-signed
     * or untrusted certificates. Defaults to false.
     */
    @JvmStatic
    @JvmOverloads
    fun fromUrl(url: URL, insecure: Boolean = false) = HttpsTestTarget(url.host,
      if (url.port == -1) 443 else url.port, if (url.path == null) "/" else url.path, insecure)
  }
}

/**
 * Test target for use with asynchronous providers (like with message queues).
 *
 * This target will look for methods with a @PactVerifyProvider annotation where the value is the description of the
 * interaction.
 *
 * @property packagesToScan List of packages to scan for methods with @PactVerifyProvider annotations. Defaults to the
 * full test classpath.
 */
open class AmpqTestTarget(val packagesToScan: List<String> = emptyList()) : TestTarget {
  override fun isHttpTarget() = false

  override fun getProviderInfo(serviceName: String, pactSource: PactSource?): ProviderInfo {
    val providerInfo = ProviderInfo(serviceName)
    providerInfo.verificationType = PactVerification.ANNOTATED_METHOD
    providerInfo.packagesToScan = packagesToScan

    if (pactSource is PactBrokerSource<*>) {
      val (_, _, _, pacts) = pactSource
      providerInfo.consumers = pacts.entries.flatMap { e -> e.value.map { p -> ConsumerInfo(e.key.name, p) } }
        .toMutableList()
    } else if (pactSource is DirectorySource<*>) {
      val (_, pacts) = pactSource
      providerInfo.consumers = pacts.entries.map { e -> ConsumerInfo(e.value.consumer.name, e.value) }
        .toMutableList()
    }
    return providerInfo
  }

  override fun prepareRequest(interaction: Interaction, context: Map<String, Any>): Pair<Any, Any>? {
    if (interaction is Message) {
      return null
    }
    throw UnsupportedOperationException("Only message interactions can be used with an AMPQ test target")
  }

  override fun prepareVerifier(verifier: IProviderVerifier, testInstance: Any) {
    verifier.projectClasspath = Supplier {
      when (val classLoader = testInstance.javaClass.classLoader) {
        is URLClassLoader -> classLoader.urLs.toList()
        else -> emptyList()
      }
    }
    val defaultProviderMethodInstance = verifier.providerMethodInstance
    verifier.providerMethodInstance = Function { m ->
      if (m.declaringClass == testInstance.javaClass) {
        testInstance
      } else {
        defaultProviderMethodInstance.apply(m)
      }
    }
  }

  override fun executeInteraction(client: Any?, request: Any?): Map<String, Any> {
    return emptyMap()
  }
}
