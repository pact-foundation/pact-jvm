package au.com.dius.pact.provider.spring.junit5

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.SynchronousRequestResponse
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.RouterFunction

class WebFluxTarget(private val routerFunction: RouterFunction<*>) : WebFluxBasedTestTarget {
  override fun prepareRequest(interaction: Interaction, context: MutableMap<String, Any>): Pair<WebTestClient.RequestHeadersSpec<*>, WebTestClient> {
    if (interaction is SynchronousRequestResponse) {
      val request = interaction.request.generatedRequest(context, GeneratorTestMode.Provider)
      val webClient = WebTestClient.bindToRouterFunction(routerFunction).build()
      return toWebFluxRequestBuilder(webClient, request) to webClient
    }
    throw UnsupportedOperationException("Only request/response interactions can be used with a WebFlux test target")
  }
}
