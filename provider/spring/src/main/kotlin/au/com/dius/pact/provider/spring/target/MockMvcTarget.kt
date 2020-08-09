package au.com.dius.pact.provider.spring.target

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.spring.MvcProviderVerifier
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder

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
  runTimes: Int = 1,
  var mockMvc: MockMvc? = null,
  var servletPath: String? = null
) : MockTestingTarget(runTimes) {

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
  override fun testInteraction(
    consumerName: String,
    interaction: Interaction,
    source: PactSource,
    context: Map<String, Any>
  ) {
    val mockMvc = buildMockMvc()
    doTestInteraction(consumerName, interaction, source) { provider, consumer, verifier, failures ->
      val mvcVerifier = verifier as MvcProviderVerifier
      mvcVerifier.verifyResponseFromProvider(provider, interaction as RequestResponseInteraction, interaction.description,
              failures, mockMvc, consumer.pending)
    }
  }

  private fun buildMockMvc(): MockMvc {
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

  override fun getRequestClass(): Class<*> = MockHttpServletRequestBuilder::class.java

  override fun createProviderVerifier() = MvcProviderVerifier(printRequestResponse)
}
