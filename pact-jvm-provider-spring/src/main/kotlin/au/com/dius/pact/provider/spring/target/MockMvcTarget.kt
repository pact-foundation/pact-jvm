package au.com.dius.pact.provider.spring.target

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.junit.Provider
import au.com.dius.pact.provider.junit.TargetRequestFilter
import au.com.dius.pact.provider.junit.target.BaseTarget
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.spring.MvcProviderVerifier
import org.apache.http.HttpRequest
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import java.net.URLClassLoader
import java.util.HashMap
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Out-of-the-box implementation of [Target],
 * that run [RequestResponseInteraction] against Spring MockMVC controllers and verify response
 *
 * To sets the servlet path on the default request, if one is required, set the servletPath to the servlet path prefix
 */
class MockMvcTarget @JvmOverloads constructor(
  var controllers: List<Any> = mutableListOf(),
  var controllerAdvice: List<Any> = mutableListOf(),
  var messageConverters: List<HttpMessageConverter<*>> = mutableListOf(),
  var printRequestResponse: Boolean = false,
  var runTimes: Int = 1,
  var mockMvc: MockMvc? = null,
  var servletPath: String? = null
) : BaseTarget() {

  fun setControllers(vararg controllers: Any) {
    this.controllers = controllers.asList()
  }

  fun setControllerAdvice(vararg controllerAdvice: Any) {
    this.controllerAdvice = controllerAdvice.asList()
  }

  fun setMessageConvertors(vararg messageConverters: HttpMessageConverter<*>) {
    this.messageConverters = messageConverters.asList()
  }

  /**
   * {@inheritDoc}
   */
  override fun testInteraction(consumerName: String, interaction: Interaction, source: PactSource) {
    val provider = getProviderInfo(source)
    val consumer = ConsumerInfo(consumerName)
    provider.verificationType = PactVerification.ANNOTATED_METHOD

    val mockMvc = buildMockMvc()

    val verifier = setupVerifier(interaction, provider, consumer) as MvcProviderVerifier

    val failures = HashMap<String, Any>()

    1.rangeTo(runTimes).forEach {
      verifier.verifyResponseFromProvider(provider, interaction as RequestResponseInteraction, interaction.description,
        failures, mockMvc)
    }

    reportTestResult(failures.isEmpty(), verifier)

    try {
      if (failures.isNotEmpty()) {
        verifier.displayFailures(failures)
        throw getAssertionError(failures)
      }
    } finally {
      verifier.finialiseReports()
    }
  }

  fun buildMockMvc(): MockMvc {
    if (mockMvc != null) {
      return mockMvc!!
    }

    val requestBuilder = MockMvcRequestBuilders.get("/")
    if (!servletPath.isNullOrEmpty()) {
      requestBuilder.servletPath(servletPath)
    }

    return standaloneSetup(*controllers.toTypedArray())
      .setControllerAdvice(*controllerAdvice.toTypedArray())
      .setMessageConverters(*messageConverters.toTypedArray())
      .defaultRequest<StandaloneMockMvcBuilder>(requestBuilder)
      .build()
  }

  override fun setupVerifier(interaction: Interaction, provider: ProviderInfo, consumer: ConsumerInfo):
          ProviderVerifier {
    val verifier = MvcProviderVerifier(printRequestResponse)

    setupReporters(verifier, provider.name, interaction.description)

    verifier.projectClasspath = Supplier { (ClassLoader.getSystemClassLoader() as URLClassLoader).urLs }

    verifier.initialiseReporters(provider)
    verifier.reportVerificationForConsumer(consumer, provider)

    if (!interaction.providerStates.isEmpty()) {
      for ((name) in interaction.providerStates) {
        verifier.reportStateForInteraction(name, provider, consumer, true)
      }
    }

    verifier.reportInteractionDescription(interaction)

    return verifier
  }

  override fun getProviderInfo(source: PactSource): ProviderInfo {
    val provider = testClass.getAnnotation(Provider::class.java)
    val providerInfo = ProviderInfo(provider.value)

    if (testClass != null) {
      val methods = testClass.getAnnotatedMethods(TargetRequestFilter::class.java)
      if (methods.isNotEmpty()) {
        providerInfo.setRequestFilter(Consumer<HttpRequest> { httpRequest ->
          methods.forEach { method ->
            try {
              method.invokeExplosively(testTarget, httpRequest)
            } catch (t: Throwable) {
              throw AssertionError("Request filter method ${method.name} failed with an exception", t)
            }
          }
        })
      }
    }

    return providerInfo
  }
}
