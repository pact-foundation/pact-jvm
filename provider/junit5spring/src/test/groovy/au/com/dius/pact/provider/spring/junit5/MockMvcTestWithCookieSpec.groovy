package au.com.dius.pact.provider.spring.junit5

import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(controllers = [ CookieResource ])
@Provider('CookieService')
@PactFolder('pacts')
class MockMvcTestWithCookieSpec {

  @BeforeEach
  void before(PactVerificationContext context) {
    context?.target = new MockMvcTestTarget(null, [ new CookieResource() ], [], [], true)
  }

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider)
  void pactVerificationTestTemplate(PactVerificationContext context, MockHttpServletRequestBuilder request) {
    request.header('test', 'test')
    context?.verifyInteraction()
  }

  @RestController
  static class CookieResource {
    @GetMapping(value = '/cookie', produces = 'text/plain')
    @ResponseStatus(HttpStatus.OK)
    String getData(@RequestParam('id') String id, @CookieValue('token') String token) {
      assert token != null && !token.empty
      "Hello $id $token"
    }
  }
}
