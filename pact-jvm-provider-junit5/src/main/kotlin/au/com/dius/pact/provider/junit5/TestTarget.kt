package au.com.dius.pact.provider.junit5

import au.com.dius.pact.model.DirectorySource
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.PactBrokerSource
import au.com.dius.pact.model.PactSource
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.HttpClientFactory
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import org.apache.http.client.methods.HttpUriRequest
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.function.Supplier
import java.util.function.Function

interface TestTarget {
  fun getProviderInfo(serviceName: String, pactSource: PactSource? = null): ProviderInfo
  fun prepareRequest(interaction: Interaction): Pair<Any, Any>?
  fun isHttpTarget(): Boolean
  fun executeInteraction(client: Any?, request: Any?): Map<String, Any>
  fun prepareVerifier(verifier: ProviderVerifier, testInstance: Any)
}

open class HttpTestTarget @JvmOverloads constructor (
  val host: String = "localhost",
  val port: Int = 8080,
  val path: String = "/"
) : TestTarget {
  override fun isHttpTarget() = true

  override fun getProviderInfo(serviceName: String, pactSource: PactSource?): ProviderInfo {
    val providerInfo = ProviderInfo(serviceName)
    providerInfo.setPort(port)
    providerInfo.setHost(host)
    providerInfo.setProtocol("http")
    providerInfo.setPath(path)
    return providerInfo
  }

  override fun prepareRequest(interaction: Interaction): Pair<Any, Any>? {
    val providerClient = ProviderClient(getProviderInfo("provider"), HttpClientFactory())
    if (interaction is RequestResponseInteraction) {
      return providerClient.prepareRequest(interaction.request.generatedRequest()) to providerClient
    }
    throw UnsupportedOperationException("Only request/response interactions can be used with an HTTP test target")
  }

  override fun prepareVerifier(verifier: ProviderVerifier, testInstance: Any) {
  }

  override fun executeInteraction(client: Any?, request: Any?): Map<String, Any> {
    val providerClient = client as ProviderClient
    val httpRequest = request as HttpUriRequest
    return providerClient.executeRequest(providerClient.getHttpClient(), httpRequest)
  }

  companion object {
    @JvmStatic
    fun fromUrl(url: URL) = HttpTestTarget(url.host,
        if (url.port == -1) 8080 else url.port,
        if (url.path == null) "/" else url.path)
  }
}

open class HttpsTestTarget @JvmOverloads constructor (
  host: String = "localhost",
  port: Int = 8443,
  path: String = "",
  val insecure: Boolean = false
) : HttpTestTarget(host, port, path) {

  override fun getProviderInfo(serviceName: String, pactSource: PactSource?): ProviderInfo {
    val providerInfo = super.getProviderInfo(serviceName, pactSource)
    providerInfo.setProtocol("https")
    providerInfo.isInsecure = insecure
    return providerInfo
  }

  companion object {
    @JvmStatic
    @JvmOverloads
    fun fromUrl(url: URL, insecure: Boolean = false) = HttpsTestTarget(url.host,
      if (url.port == -1) 443 else url.port, if (url.path == null) "/" else url.path, insecure)
  }
}

open class AmpqTestTarget(val packagesToScan: List<String> = emptyList()) : TestTarget {
  override fun isHttpTarget() = false

  override fun getProviderInfo(serviceName: String, pactSource: PactSource?): ProviderInfo {
    val providerInfo = ProviderInfo(serviceName)
    providerInfo.verificationType = PactVerification.ANNOTATED_METHOD
    providerInfo.packagesToScan = packagesToScan

    if (pactSource is PactBrokerSource<*>) {
      val (_, _, pacts) = pactSource
      providerInfo.consumers = pacts.entries.flatMap { e -> e.value.map { p -> ConsumerInfo(e.key.name, p) } }
    } else if (pactSource is DirectorySource<*>) {
      val (_, pacts) = pactSource
      providerInfo.consumers = pacts.entries.map { e -> ConsumerInfo(e.value.consumer.name, e.value) }
    }
    return providerInfo
  }

  override fun prepareRequest(interaction: Interaction): Pair<Any, Any>? {
    if (interaction is Message) {
      return null
    }
    throw UnsupportedOperationException("Only message interactions can be used with an Ampq test target")
  }

  override fun prepareVerifier(verifier: ProviderVerifier, testInstance: Any) {
    verifier.projectClasspath = Supplier<Array<URL>> { (ClassLoader.getSystemClassLoader() as URLClassLoader).urLs }
    val defaultProviderMethodInstance = verifier.providerMethodInstance
    verifier.providerMethodInstance = Function<Method, Any?> { m ->
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
