package au.com.dius.pact.provider.spring.junit5

import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Provider("myAwesomeService")
@IgnoreNoPactsToVerify
@PactFolder("pacts")
internal class MockMvcTestTargetNoCustomMockMvcTest {

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun pactVerificationTestTemplate(context: PactVerificationContext?) {
        context?.verifyInteraction()
    }

    @BeforeEach
    fun before(context: PactVerificationContext?) {
        context?.target = MockMvcTestTarget(controllers = listOf(DataResource()))
    }

    @RestController
    internal class DataResource {
        @GetMapping("/data")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        fun getData(@RequestParam("ticketId") ticketId: String) {
        }
    }
}
