// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 1
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.kotlin.pact
import au.com.dius.pact.consumer.kotlin.runConsumerTest
import au.com.dius.pact.consumer.dsl.newJsonObject
import org.junit.jupiter.api.Test
import java.net.URL

class README_kotlin_block01_Test {

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:1
        val myPact = pact(consumer = "MyConsumer", provider = "MyProvider") {
            uponReceiving("a request for a user") {
                given("user 1 exists")
                withRequest {
                    method("GET")
                    path("/api/users/1")
                    header("Accept", "application/json")
                }
                willRespondWith {
                    status(200)
                    header("Content-Type", "application/json")
                    body(newJsonObject {
                        stringType("id", "1")
                        stringType("name", "Alice")
                        numberType("age", 30)
                    })
                }
            }
        }
        
        val result = runConsumerTest(myPact) {
            // `this` is MockServer — call getUrl() for the base URL
            val response = URL("${getUrl()}/api/users/1").readText()
            // make assertions here
        }
        // @DOCTEST-END
    }
}
