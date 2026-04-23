// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 2
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.kotlin.pact
import au.com.dius.pact.consumer.dsl.newJsonArray
import au.com.dius.pact.consumer.dsl.newJsonObject
import au.com.dius.pact.consumer.dsl.newObject
import org.junit.jupiter.api.Test

class README_kotlin_block02_Test {

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:2
        val pact = pact(consumer = "OrderService", provider = "ProductService") {
        
            // -- interaction 1 --
            given("products exist")
            uponReceiving("a request for all products") {
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
        
            // -- interaction 2: parametrised provider state --
            given("product with id 'abc-123' exists")
            uponReceiving("a request for a single product") {
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
        
            // -- interaction 3: not found --
            given("no products exist")
            uponReceiving("a request for a product that does not exist") {
                withRequest {
                    method("GET")
                    path("/products/missing")
                }
                willRespondWith {
                    status(404)
                }
            }
        }
        // @DOCTEST-END
    }
}
