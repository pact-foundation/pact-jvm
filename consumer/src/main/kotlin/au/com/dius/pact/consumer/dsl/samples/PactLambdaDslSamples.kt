package au.com.dius.pact.consumer.dsl.samples

import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.dsl.newJsonObject
import au.com.dius.pact.core.model.RequestResponsePact

/**
 * Samples of using Lambda DSL for creating pacts.
 */
object PactLambdaDslSamples {

    /**
     * Shows how Lambda DSL can be used to visually separate the request and the response
     * section from each other.
     */
    fun requestResponse(builder: PactDslWithProvider): RequestResponsePact {
        return builder.given("no existing users")
            .uponReceiving("create a new user")
            .path("users") {
                // Lambda DSL on request, this: PactDslRequestWithPath
                headers("X-Locale", "en-US")
                method("PUT")
            }.willRespondWith {
                // Lambda DSL on response, this: PactDslResponse
                successStatus()
                body(
                    newJsonObject {
                        uuid("name")
                    }
                )
            }.toPact()
    }
}
