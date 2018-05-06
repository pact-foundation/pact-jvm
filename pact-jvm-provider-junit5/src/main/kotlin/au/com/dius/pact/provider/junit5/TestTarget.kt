package au.com.dius.pact.provider.junit5

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.provider.HttpClientFactory
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import org.apache.http.client.methods.HttpUriRequest
import java.net.URL

interface TestTarget {
  fun getProviderInfo(serviceName: String): ProviderInfo
  fun prepareRequest(interaction: Interaction): Pair<Any, Any>
  fun isHttpTarget(): Boolean
  fun executeInteraction(client: Any, request: Any): Map<String, Any>
}

data class HttpTestTarget (val host: String = "localhost", val port: Int = 8080, val path: String = "/") : TestTarget {
  override fun isHttpTarget() = true

  override fun getProviderInfo(serviceName: String): ProviderInfo {
    val providerInfo = ProviderInfo(serviceName)
    providerInfo.setPort(port)
    providerInfo.setHost(host)
    providerInfo.setProtocol("http")
    providerInfo.setPath(path)
    return providerInfo
  }

  override fun prepareRequest(interaction: Interaction): Pair<Any, Any> {
    val providerClient = ProviderClient(getProviderInfo("provider"), HttpClientFactory())
    if (interaction is RequestResponseInteraction) {
      return providerClient.prepareRequest(interaction.request.generatedRequest()) to providerClient
    }
    throw UnsupportedOperationException("Only request/response interactions can be used with an HTTP test target")
  }

  override fun executeInteraction(client: Any, request: Any): Map<String, Any> {
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

data class HttpsTestTarget(val host: String = "localhost", val port: Int = 8443, val path: String = "", val insecure: Boolean = false) : TestTarget {
  override fun isHttpTarget() = true

  override fun getProviderInfo(serviceName: String): ProviderInfo {
    val providerInfo = ProviderInfo(serviceName)
    providerInfo.setPort(port)
    providerInfo.setHost(host)
    providerInfo.setProtocol("https")
    providerInfo.setPath(path)
    providerInfo.isInsecure = insecure
    return providerInfo
  }

  override fun prepareRequest(interaction: Interaction): Pair<Any, Any> {
    val providerClient = ProviderClient(getProviderInfo("provider"), HttpClientFactory())
    if (interaction is RequestResponseInteraction) {
      return providerClient.prepareRequest(interaction.request.generatedRequest()) to providerClient
    }
    throw UnsupportedOperationException("Only request/response interactions can be used with an HTTPS test target")
  }

  override fun executeInteraction(client: Any, request: Any): Map<String, Any> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  companion object {
    @JvmStatic
    @JvmOverloads
    fun fromUrl(url: URL, insecure: Boolean = false) = HttpsTestTarget(url.host,
      if (url.port == -1) 443 else url.port, if (url.path == null) "/" else url.path, insecure)
  }
}

data class AmpqTestTarget(val packagesToScan: List<String> = emptyList()) : TestTarget {
  override fun isHttpTarget() = false

  override fun getProviderInfo(serviceName: String): ProviderInfo {
    val providerInfo = ProviderInfo(serviceName)
    providerInfo.verificationType = PactVerification.ANNOTATED_METHOD
    providerInfo.packagesToScan = packagesToScan

//    if (source is PactBrokerSource<*>) {
//      val (_, _, pacts) = source
//      providerInfo.consumers = pacts.entries.flatMap { e -> e.value.map { p -> ConsumerInfo(e.key.name, p) } }
//    } else if (source is DirectorySource<*>) {
//      val (_, pacts) = source
//      providerInfo.consumers = pacts.entries.map { e -> ConsumerInfo(e.value.consumer.name, e.value) }
//    }
    return providerInfo
  }

  override fun prepareRequest(interaction: Interaction): Pair<Any, Any> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun executeInteraction(client: Any, request: Any): Map<String, Any> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
