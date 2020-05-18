package au.com.dius.pact.provider.junit.target

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.provider.HttpClientFactory
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.junit.Provider
import au.com.dius.pact.provider.junit.TargetRequestFilter
import org.apache.http.HttpRequest
import java.net.URL
import java.util.function.Consumer

/**
 * Out-of-the-box implementation of [Target],
 * that run [Interaction] against http service and verify response
 */
open class HttpTarget
  /**
   *
   * @param host host of tested service
   * @param port port of tested service
   * @param protocol protocol of the tested service
   * @param path path of the tested service
   * @param insecure true if certificates should be ignored
   */
  @JvmOverloads constructor(
    val protocol: String = "http",
    val host: String = "127.0.0.1",
    open val port: Int = 8080,
    val path: String = "/",
    val insecure: Boolean = false
  ) : BaseTarget() {

  /**
   * @param port port of tested service
   */
  @JvmOverloads constructor(host: String = "127.0.0.1", port: Int) : this("http", host, port)

  /**
   * @param url url of the tested service
   * @param insecure true if certificates should be ignored
   */
  @JvmOverloads constructor(url: URL, insecure: Boolean = false) : this(
    if (url.protocol == null) "http" else url.protocol,
    url.host,
    if (url.port == -1 && url.protocol.equals("http", ignoreCase = true)) 8080
    else if (url.port == -1 && url.protocol.equals("https", ignoreCase = true)) 443
    else url.port,
    if (url.path == null) "/" else url.path,
    insecure
  )

  /**
   * {@inheritDoc}
   */
  override fun testInteraction(
    consumerName: String,
    interaction: Interaction,
    source: PactSource,
    context: Map<String, Any>
  ) {
    val provider = getProviderInfo(source)
    val consumer = consumerInfo(consumerName, source)
    val verifier = setupVerifier(interaction, provider, consumer, source)

    val client = ProviderClient(provider, HttpClientFactory())
    val result = verifier.verifyResponseFromProvider(provider, interaction as RequestResponseInteraction,
      interaction.description, mutableMapOf(), client, context, consumer.pending)
    reportTestResult(result, verifier)

    try {
      if (result is VerificationResult.Failed) {
        verifier.displayFailures(listOf(result))
        throw AssertionError(verifier.generateErrorStringFromVerificationResult(listOf(result)))
      }
    } finally {
      verifier.finaliseReports()
    }
  }

  override fun setupVerifier(
    interaction: Interaction,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    pactSource: PactSource?
  ): IProviderVerifier {
    val verifier = ProviderVerifier()

    setupReporters(verifier)

    verifier.initialiseReporters(provider)
    verifier.reportVerificationForConsumer(consumer, provider, pactSource)

    if (interaction.providerStates.isNotEmpty()) {
      for ((name) in interaction.providerStates) {
        verifier.reportStateForInteraction(name.toString(), provider, consumer, true)
      }
    }

    verifier.reportInteractionDescription(interaction)

    return verifier
  }

  override fun getProviderInfo(source: PactSource): ProviderInfo {
    val provider = testClass.getAnnotation(Provider::class.java)
    val providerInfo = ProviderInfo(provider.value)
    providerInfo.port = port
    providerInfo.host = host
    providerInfo.protocol = protocol
    providerInfo.path = path
    providerInfo.insecure = insecure

    val methods = testClass.getAnnotatedMethods(TargetRequestFilter::class.java)
    if (methods.isNotEmpty()) {
      validateTargetRequestFilters(methods)

      providerInfo.requestFilter = Consumer { httpRequest: HttpRequest ->
        methods.forEach { method ->
          try {
            method.invokeExplosively(testTarget, httpRequest)
          } catch (t: Throwable) {
            throw AssertionError("Request filter method ${method.name} failed with an exception", t)
          }
        }
      }
    }

    return providerInfo
  }
}
