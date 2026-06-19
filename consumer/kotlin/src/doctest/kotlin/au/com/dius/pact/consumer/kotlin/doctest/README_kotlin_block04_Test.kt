// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 4
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.kotlin.pact
import au.com.dius.pact.consumer.dsl.newJsonArray
import au.com.dius.pact.consumer.dsl.newJsonObject
import au.com.dius.pact.consumer.dsl.newObject
import org.junit.jupiter.api.Test

class README_kotlin_block04_Test {

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:4
        val pact = pact(consumer = "OrderService", provider = "ProductService") {
        
            interaction("get all products") {
                given("products exist")
                withRequest {
                    method("GET")
                    path("/products")
                }
                willRespondWith {
                    status(200)
                    body(newJsonArray {
                        newObject {
                            stringType("id", "abc-123")
                            stringType("name", "Widget")
                            numberType("price", 9.99)
                        }
                    })
                }
            }
        
            interaction("get a single product") {
                given("product with id 'abc-123' exists", "id" to "abc-123")
                withRequest {
                    method("GET")
                    path("/products/abc-123")
                }
                willRespondWith {
                    status(200)
                    body(newJsonObject {
                        stringType("id", "abc-123")
                        stringType("name", "Widget")
                    })
                }
            }
        }
        // @DOCTEST-END
    }
}
