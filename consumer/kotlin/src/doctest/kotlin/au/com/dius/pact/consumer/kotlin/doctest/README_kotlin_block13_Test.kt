// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 13
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.HttpResponseBuilder
import au.com.dius.pact.core.model.HttpResponse
import org.junit.jupiter.api.Test

class README_kotlin_block13_Test {

    private fun willRespondWith(block: HttpResponseBuilder.() -> Unit) {
        HttpResponseBuilder(HttpResponse()).apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:13
        willRespondWith { successStatus() }       // matches 200–299
        willRespondWith { clientErrorStatus() }   // matches 400–499
        willRespondWith { serverErrorStatus() }   // matches 500–599
        willRespondWith { nonErrorStatus() }      // matches < 400
        willRespondWith { errorStatus() }         // matches >= 400
        willRespondWith { redirectStatus() }      // matches 300–399
        willRespondWith { informationStatus() }   // matches 100–199
        willRespondWith { statusCodes(listOf(200, 201, 204)) }
        // @DOCTEST-END
    }
}
