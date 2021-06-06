package au.com.dius.pact.provider.junit.target

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.provider.HttpClientFactory
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IHttpClientFactory
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderUtils
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.TargetRequestFilter
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
    val insecure: Boolean = false,
    val httpClientFactory: () -> IHttpClientFactory = { HttpClientFactory() }
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
    context: MutableMap<String, Any>,
    pending: Boolean
  ) {
    val client = ProviderClient(provider, this.httpClientFactory.invoke())

    val requestResponse = interaction.asSynchronousRequestResponse()
    val result = if (requestResponse == null) {
      val message = "HttpTarget can only be used with Request/Response interactions, got $interaction"
      VerificationResult.Failed(message, message,
        mapOf(
          interaction.interactionId.orEmpty() to listOf(VerificationFailureType.InvalidInteractionFailure(message))
        ), pending)
    } else {
      verifier.verifyResponseFromProvider(provider, requestResponse, interaction.description, mutableMapOf(), client,
        context, pending)
    }

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

    return verifier
  }

  override fun getProviderInfo(source: PactSource): ProviderInfo {
    val provider = ProviderUtils.findAnnotation(testClass.javaClass, Provider::class.java)!!
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
