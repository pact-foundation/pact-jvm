// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 8
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.HttpRequestBuilder
import au.com.dius.pact.consumer.dsl.Matchers.regexp
import au.com.dius.pact.core.model.HttpRequest
import org.junit.jupiter.api.Test

class README_kotlin_block08_Test {

    private fun withRequest(block: HttpRequestBuilder.() -> Unit) {
        HttpRequestBuilder(HttpRequest()).apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:8
        withRequest {
            method("GET")
            path("/api/users")
            queryParameter("page", "1")
            queryParameter("size", "20")
            queryParameter("sort", regexp("[a-z]+,(asc|desc)", "name,asc"))
        }
        // @DOCTEST-END
    }
}
