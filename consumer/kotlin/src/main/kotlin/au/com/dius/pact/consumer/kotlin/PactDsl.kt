package au.com.dius.pact.consumer.kotlin

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.consumer.PactTestRun
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.dsl.HttpInteractionBuilder
import au.com.dius.pact.consumer.dsl.HttpRequestBuilder
import au.com.dius.pact.consumer.dsl.HttpResponseBuilder
import au.com.dius.pact.consumer.dsl.PactBuilder
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.consumer.runConsumerTest as baseRunConsumerTest

/**
 * DSL marker to prevent implicit receiver leakage in nested Pact DSL blocks.
 */
@DslMarker
annotation class PactDslMarker

/**
 * Creates a V4 Pact using an idiomatic Kotlin DSL.
 *
 * Example:
 * ```kotlin
 * val myPact = pact(consumer = "MyConsumer", provider = "MyProvider") {
 *     uponReceiving("a request for users") {
 *         given("users exist")
 *         withRequest {
 *             method("GET")
 *             path("/api/users")
 *             header("Accept", "application/json")
 *         }
 *         willRespondWith {
 *             status(200)
 *             header("Content-Type", "application/json")
 *             body(newJsonArray {
 *                 newObject {
 *                     stringType("name", "Alice")
 *                     numberType("age", 30)
 *                 }
 *             })
 *         }
 *     }
 * }
 * ```
 */
fun pact(consumer: String, provider: String, block: ConsumerPactDsl.() -> Unit): V4Pact {
    val dsl = ConsumerPactDsl(consumer, provider)
    dsl.block()
    return dsl.toPact()
}

/**
 * Receiver for the top-level [pact] DSL block. Accumulates interactions and pact-level settings.
 */
@PactDslMarker
class ConsumerPactDsl(consumer: String, provider: String) {
    private val builder = PactBuilder(consumer, provider)

    /**
     * Enables a Pact plugin by name. Optionally pins a specific version.
     */
    fun usingPlugin(name: String, version: String? = null) {
        builder.usingPlugin(name, version)
    }

    /**
     * Adds an entry to the metadata section of the Pact file.
     */
    fun addMetadata(key: String, value: String) {
        builder.addMetadataValue(key, value)
    }

    /**
     * Adds a text comment. The comment is attached to the next interaction.
     */
    fun comment(comment: String) {
        builder.comment(comment)
    }

    /**
     * Describes the provider state required for the next interaction. May be called multiple times.
     * Parameters will be applied to the next [uponReceiving] or [interaction] block.
     */
    fun given(state: String, params: Map<String, Any?> = emptyMap()) {
        builder.given(state, params)
    }

    /**
     * Describes the provider state required for the next interaction with key/value parameters.
     */
    fun given(state: String, vararg params: Pair<String, Any?>) {
        builder.given(state, params.toMap())
    }

    /**
     * Defines an HTTP interaction using a BDD-style description. Equivalent to [interaction].
     */
    fun uponReceiving(description: String, block: HttpInteractionDsl.() -> Unit) {
        interaction(description, block)
    }

    /**
     * Defines an HTTP interaction. The [block] configures the expected request and response.
     */
    fun interaction(description: String, block: HttpInteractionDsl.() -> Unit) {
        val interactionDsl = HttpInteractionDsl()
        interactionDsl.block()
        builder.expectsToReceiveHttpInteraction(description) { interactionDsl.applyTo(it) }
    }

    internal fun toPact(): V4Pact = builder.toPact()
}

/**
 * Receiver for an [interaction] or [uponReceiving] block. Configures a single HTTP interaction.
 */
@PactDslMarker
class HttpInteractionDsl {
    private val states = mutableListOf<Pair<String, Map<String, Any?>>>()
    private var isPending = false
    private val interactionComments = mutableListOf<String>()
    private var requestBlock: (HttpRequestBuilder.() -> Unit)? = null
    private var responseBlock: (HttpResponseBuilder.() -> Unit)? = null

    /**
     * Adds a provider state to this interaction. May be called multiple times for multiple states.
     */
    fun given(state: String, params: Map<String, Any?> = emptyMap()) {
        states.add(state to params)
    }

    /**
     * Adds a provider state with key/value parameters to this interaction.
     */
    fun given(state: String, vararg params: Pair<String, Any?>) {
        states.add(state to params.toMap())
    }

    /**
     * Marks this interaction as pending. A pending interaction will not cause provider verification to fail.
     */
    fun pending(pending: Boolean) {
        isPending = pending
    }

    /**
     * Adds a text comment to the interaction.
     */
    fun comment(comment: String) {
        interactionComments.add(comment)
    }

    /**
     * Configures the expected HTTP request. The [block] receives an [HttpRequestBuilder] as its receiver,
     * giving access to [HttpRequestBuilder.method], [HttpRequestBuilder.path],
     * [HttpRequestBuilder.header], [HttpRequestBuilder.queryParameter], [HttpRequestBuilder.body], and more.
     */
    fun withRequest(block: HttpRequestBuilder.() -> Unit) {
        requestBlock = block
    }

    /**
     * Configures the expected HTTP response. The [block] receives an [HttpResponseBuilder] as its receiver,
     * giving access to [HttpResponseBuilder.status], [HttpResponseBuilder.header],
     * [HttpResponseBuilder.body], and semantic status matchers like [HttpResponseBuilder.successStatus].
     */
    fun willRespondWith(block: HttpResponseBuilder.() -> Unit) {
        responseBlock = block
    }

    internal fun applyTo(builder: HttpInteractionBuilder): HttpInteractionBuilder {
        for ((state, params) in states) {
            builder.state(state, params)
        }
        if (isPending) {
            builder.pending(true)
        }
        for (comment in interactionComments) {
            builder.comment(comment)
        }
        requestBlock?.let { block -> builder.withRequest { it.apply(block) } }
        responseBlock?.let { block -> builder.willRespondWith { it.apply(block) } }
        return builder
    }
}

/**
 * Runs a consumer Pact test against a mock HTTP server and writes the Pact file on success.
 *
 * The [test] block receives the [MockServer] as its receiver; call [MockServer.getUrl] to obtain
 * the base URL for making requests against the mock.
 *
 * Example:
 * ```kotlin
 * val result = runConsumerTest(myPact) {
 *     val response = URL("${getUrl()}/api/users").readText()
 *     assertThat(response, containsString("Alice"))
 * }
 * assertThat(result, instanceOf(PactVerificationResult.Ok::class.java))
 * ```
 */
fun <R> runConsumerTest(
    pact: V4Pact,
    config: MockProviderConfig = MockProviderConfig.createDefault(PactSpecVersion.V4),
    test: MockServer.() -> R
): PactVerificationResult {
    return baseRunConsumerTest(pact, config, object : PactTestRun<R> {
        override fun run(mockServer: MockServer, context: PactTestExecutionContext?): R = test(mockServer)
    })
}
