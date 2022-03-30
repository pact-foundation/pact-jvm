package au.com.dius.pact.provider.spring.junit5

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.SynchronousRequestResponse
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import org.springframework.test.web.reactive.server.WebTestClient

class WebTestClientTarget(private val webTestClient: WebTestClient) : WebFluxBasedTestTarget {
    override val userConfig: Map<String, Any?> = emptyMap()

    override fun prepareRequest(
        pact: Pact,
        interaction: Interaction,
        context: MutableMap<String, Any>
    ): Pair<Any, Any>? {
        if (interaction is SynchronousRequestResponse) {
            val request = interaction.request.generatedRequest(context, GeneratorTestMode.Provider)
            return toWebFluxRequestBuilder(webTestClient, request) to webTestClient
        }
        throw UnsupportedOperationException("Only request/response interactions can be used with a WebFlux test target")
    }
}
