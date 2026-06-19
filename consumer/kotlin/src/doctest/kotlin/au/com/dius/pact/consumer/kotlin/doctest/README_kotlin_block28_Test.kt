// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 28
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.kotlin.pact
import au.com.dius.pact.consumer.kotlin.runConsumerTest
import au.com.dius.pact.consumer.PactVerificationResult
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.URL

class README_kotlin_block28_Test {

    private val myPact = pact(consumer = "Block28Consumer", provider = "Block28Provider") {
        uponReceiving("a request for a user") {
            withRequest {
                method("GET")
                path("/api/users/1")
                header("Accept", "application/json")
            }
            willRespondWith {
                status(200)
            }
        }
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:28
        val result = runConsumerTest(myPact) {
            // `this` is MockServer
            val baseUrl = getUrl()
        
            val connection = URL("$baseUrl/api/users/1").openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/json")
            assertThat(connection.responseCode, equalTo(200))
        }
        
        assertThat(result, instanceOf(PactVerificationResult.Ok::class.java))
        // @DOCTEST-END
    }
}
