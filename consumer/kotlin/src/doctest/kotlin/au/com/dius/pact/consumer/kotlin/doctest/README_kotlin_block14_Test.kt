// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 14
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.HttpResponseBuilder
import au.com.dius.pact.consumer.dsl.Matchers.uuid
import au.com.dius.pact.core.model.HttpResponse
import org.junit.jupiter.api.Test

class README_kotlin_block14_Test {

    private fun willRespondWith(block: HttpResponseBuilder.() -> Unit) {
        HttpResponseBuilder(HttpResponse()).apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:14
        willRespondWith {
            status(200)
            header("Content-Type", "application/json")
            header("X-Request-Id", uuid())
        }
        // @DOCTEST-END
    }
}
