package au.com.dius.pact.provider.spring

import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Request
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup

class MvcProviderVerifierSpec extends Specification {

  private MvcProviderVerifier verifier
  private MockMvc mockMvc

  @RestController
  static class MockController {
    @RequestMapping('/')
    def get(@RequestBody String body) {
      body
    }

    @RequestMapping('/upload')
    def upload(@RequestParam('file') MultipartFile body) {
      [body.name, body.contentType, body.originalFilename, body.inputStream.text].join('|')
    }
  }

  def setup() {
    verifier = new MvcProviderVerifier()
    mockMvc = standaloneSetup(new MockController()).build()
  }

  def 'executing a request against mock MVC with a body'() {
    given:
    def body = '"This is a body"'
    def request = new Request(body: OptionalBody.body(body.bytes))

    when:
    def response = verifier.executeMockMvcRequest(mockMvc, request)

    then:
    response.response.contentType == 'text/plain;charset=ISO-8859-1'
    response.response.contentAsString == body
  }

  def 'executing a request against mock MVC with no body'() {
    given:
    def request = new Request()

    when:
    def response = verifier.executeMockMvcRequest(mockMvc, request)

    then:
    response.response.contentType == null
    response.response.contentAsString == ''
  }

  def 'executing a request against mock MVC with a multipart file upload'() {
    given:
    def request = new Request(path: '/upload').withMultipartFileUpload('file', 'filename', 'text/csv', 'file,contents')

    when:
    def response = verifier.executeMockMvcRequest(mockMvc, request)

    then:
    response.response.contentType == 'text/plain;charset=ISO-8859-1'
    response.response.contentAsString == 'file|text/csv|filename|file,contents'
  }

}
