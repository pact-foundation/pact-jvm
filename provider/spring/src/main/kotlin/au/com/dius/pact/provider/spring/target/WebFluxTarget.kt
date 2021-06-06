package au.com.dius.pact.provider.spring.target

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.spring.WebFluxProviderVerifier
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.RouterFunction

class WebFluxTarget(
  runTimes: Int = 1
) : MockTestingTarget(runTimes) {

  var controllers = listOf<Any>()
  var routerFunction: RouterFunction<*>? = null

  override fun testInteraction(
    consumerName: String,
    interaction: Interaction,
    source: PactSource,
    context: MutableMap<String, Any>,
    pending: Boolean
  ) {
    doTestInteraction(consumerName, interaction, source) { provider, consumer, verifier, failures ->
      val webClient = routerFunction?.let {
        WebTestClient.bindToRouterFunction(routerFunction).build()
      } ?: WebTestClient.bindToController(*controllers.toTypedArray()).build()
      val webFluxProviderVerifier = verifier as WebFluxProviderVerifier
      val requestResponse = interaction.asSynchronousRequestResponse()
      if (requestResponse == null) {
        val message = "WebFluxTarget can only be used with Request/Response interactions, got $interaction"
        VerificationResult.Failed(message, message,
          mapOf(
            interaction.interactionId.orEmpty() to listOf(VerificationFailureType.InvalidInteractionFailure(message))
          ), pending)
      } else {
        webFluxProviderVerifier.verifyResponseFromProvider(provider, requestResponse, interaction.description,
          failures, webClient, consumer.pending
        )
      }
    }
  }

  override fun getRequestClass(): Class<*> = WebTestClient.RequestHeadersSpec::class.java

  override fun createProviderVerifier() = WebFluxProviderVerifier()
}
