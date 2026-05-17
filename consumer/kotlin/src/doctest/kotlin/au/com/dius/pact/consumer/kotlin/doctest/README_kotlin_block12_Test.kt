// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 12
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.HttpResponseBuilder
import au.com.dius.pact.core.model.HttpResponse
import org.junit.jupiter.api.Test

class README_kotlin_block12_Test {

    private fun willRespondWith(block: HttpResponseBuilder.() -> Unit) {
        HttpResponseBuilder(HttpResponse()).apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:12
        willRespondWith { status(201) }
        // @DOCTEST-END
    }
}
