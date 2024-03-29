package au.com.dius.pact.provider.spring.spring6

import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.DeferredResult
import java.util.concurrent.CompletableFuture

@Provider("myAwesomeService")
@IgnoreNoPactsToVerify
@PactFolder("pacts")
internal class MockMvcTestTargetStandaloneMockMvcTest {

    val mockMvc = MockMvcBuilders.standaloneSetup(DataResource()).build()

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun pactVerificationTestTemplate(context: PactVerificationContext?) {
        context?.verifyInteraction()
    }

    @BeforeEach
    fun before(context: PactVerificationContext?) {
        context?.target = Spring6MockMvcTestTarget(mockMvc)
    }

    @RestController
    internal class DataResource {
        @GetMapping("/data")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        fun getData(@RequestParam("ticketId") ticketId: String) {
        }

        @GetMapping("/async-data")
        fun getAsyncData(@RequestParam("ticketId") ticketId: String): DeferredResult<ResponseEntity<Any>> {
            val result = DeferredResult<ResponseEntity<Any>>()
            CompletableFuture.runAsync {
                result.setResult(ResponseEntity
                        .noContent()
                        .build())
            }
            return result
        }
    }
}
