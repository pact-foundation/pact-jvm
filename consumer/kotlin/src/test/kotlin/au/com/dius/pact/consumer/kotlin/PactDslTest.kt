package au.com.dius.pact.consumer.kotlin

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.dsl.newJsonArray
import au.com.dius.pact.consumer.dsl.newJsonObject
import au.com.dius.pact.consumer.dsl.newObject
import au.com.dius.pact.core.model.V4Interaction
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.URL

class PactDslTest {

    @Test
    fun `creates a pact with consumer and provider names`() {
        val myPact = pact(consumer = "MyConsumer", provider = "MyProvider") {
            uponReceiving("a request") {
                withRequest { method("GET"); path("/") }
                willRespondWith { status(200) }
            }
        }

        assertThat(myPact.consumer.name, `is`("MyConsumer"))
        assertThat(myPact.provider.name, `is`("MyProvider"))
    }

    @Test
    fun `creates a pact with a single HTTP interaction`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            uponReceiving("a request for a user") {
                given("user with id 1 exists")
                withRequest {
                    method("GET")
                    path("/api/users/1")
                    header("Accept", "application/json")
                }
                willRespondWith {
                    status(200)
                    header("Content-Type", "application/json")
                    body("""{"id": 1, "name": "Alice"}""")
                }
            }
        }

        assertThat(myPact.interactions, hasSize(1))
        val interaction = myPact.interactions[0] as V4Interaction.SynchronousHttp
        assertThat(interaction.description, `is`("a request for a user"))
        assertThat(interaction.providerStates.size, `is`(1))
        assertThat(interaction.providerStates[0].name, `is`("user with id 1 exists"))
        assertThat(interaction.request.method, `is`("GET"))
        assertThat(interaction.request.path, `is`("/api/users/1"))
        assertThat(interaction.response.status, `is`(200))
    }

    @Test
    fun `creates a pact with provider state parameters`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            uponReceiving("a request with state params") {
                given("a user exists", "id" to "42", "name" to "Bob")
                withRequest { method("GET"); path("/api/users/42") }
                willRespondWith { status(200) }
            }
        }

        val interaction = myPact.interactions[0] as V4Interaction.SynchronousHttp
        assertThat(interaction.providerStates[0].name, `is`("a user exists"))
        assertThat(interaction.providerStates[0].params["id"], `is`("42"))
        assertThat(interaction.providerStates[0].params["name"], `is`("Bob"))
    }

    @Test
    fun `creates a pact with multiple provider states on a single interaction`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            uponReceiving("a request requiring multiple states") {
                given("users exist")
                given("the system is healthy")
                withRequest { method("GET"); path("/api/users") }
                willRespondWith { status(200) }
            }
        }

        val interaction = myPact.interactions[0] as V4Interaction.SynchronousHttp
        assertThat(interaction.providerStates, hasSize(2))
        assertThat(interaction.providerStates[0].name, `is`("users exist"))
        assertThat(interaction.providerStates[1].name, `is`("the system is healthy"))
    }

    @Test
    fun `creates a pact with multiple interactions`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            uponReceiving("get all users") {
                withRequest { method("GET"); path("/api/users") }
                willRespondWith { status(200) }
            }
            uponReceiving("create a user") {
                withRequest {
                    method("POST")
                    path("/api/users")
                    body("""{"name": "Charlie"}""", "application/json")
                }
                willRespondWith { status(201) }
            }
        }

        assertThat(myPact.interactions, hasSize(2))
        assertThat(myPact.interactions[0].description, `is`("get all users"))
        assertThat(myPact.interactions[1].description, `is`("create a user"))
    }

    @Test
    fun `creates a pact with JSON body matchers`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            uponReceiving("a request for user details") {
                withRequest { method("GET"); path("/api/users/1") }
                willRespondWith {
                    status(200)
                    body(newJsonObject {
                        stringType("name", "Alice")
                        numberType("age", 30)
                        booleanType("active", true)
                    })
                }
            }
        }

        val interaction = myPact.interactions[0] as V4Interaction.SynchronousHttp
        assertThat(interaction.response.matchingRules.rulesForCategory("body").isNotEmpty(), `is`(true))
    }

    @Test
    fun `creates a pact with a JSON array body`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            uponReceiving("a request for a list of users") {
                withRequest { method("GET"); path("/api/users") }
                willRespondWith {
                    status(200)
                    body(newJsonArray {
                        newObject {
                            stringType("name", "Alice")
                        }
                    })
                }
            }
        }

        val interaction = myPact.interactions[0] as V4Interaction.SynchronousHttp
        assertThat(interaction.response.matchingRules.rulesForCategory("body").isNotEmpty(), `is`(true))
    }

    @Test
    fun `creates a pact with query parameters`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            uponReceiving("a paginated request") {
                withRequest {
                    method("GET")
                    path("/api/users")
                    queryParameter("page", "1")
                    queryParameter("size", "10")
                }
                willRespondWith { status(200) }
            }
        }

        val interaction = myPact.interactions[0] as V4Interaction.SynchronousHttp
        assertThat(interaction.request.query["page"], equalTo(listOf("1")))
        assertThat(interaction.request.query["size"], equalTo(listOf("10")))
    }

    @Test
    fun `creates a pact with a pending interaction`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            uponReceiving("a work-in-progress interaction") {
                pending(true)
                withRequest { method("GET"); path("/api/future") }
                willRespondWith { status(200) }
            }
        }

        val interaction = myPact.interactions[0] as V4Interaction.SynchronousHttp
        assertThat(interaction.pending, `is`(true))
    }

    @Test
    fun `creates a pact with pact-level provider states applied to the next interaction`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            given("global state")
            uponReceiving("a request using global state") {
                withRequest { method("GET"); path("/api/data") }
                willRespondWith { status(200) }
            }
        }

        val interaction = myPact.interactions[0] as V4Interaction.SynchronousHttp
        assertThat(interaction.providerStates[0].name, `is`("global state"))
    }

    @Test
    fun `creates a pact with semantic response status matchers`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            uponReceiving("a request that may succeed") {
                withRequest { method("GET"); path("/api/resource") }
                willRespondWith { successStatus() }
            }
        }

        val interaction = myPact.interactions[0] as V4Interaction.SynchronousHttp
        assertThat(interaction.response.matchingRules.rulesForCategory("status").isNotEmpty(), `is`(true))
    }

    @Test
    fun `supports the interaction alias for uponReceiving`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            interaction("a request via the interaction alias") {
                withRequest { method("GET"); path("/api/alias") }
                willRespondWith { status(200) }
            }
        }

        assertThat(myPact.interactions[0].description, `is`("a request via the interaction alias"))
    }

    @Test
    fun `supports running a consumer test against a mock server`() {
        val myPact = pact(consumer = "Consumer", provider = "Provider") {
            uponReceiving("a simple GET request") {
                withRequest { method("GET"); path("/ping") }
                willRespondWith { status(200) }
            }
        }

        val result = runConsumerTest(myPact) {
            val connection = URL("${getUrl()}/ping").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            assertThat(connection.responseCode, `is`(200))
        }

        assertThat(result, instanceOf(PactVerificationResult.Ok::class.java))
    }
}
