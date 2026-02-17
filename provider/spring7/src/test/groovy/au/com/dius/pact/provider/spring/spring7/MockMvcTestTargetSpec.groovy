package au.com.dius.pact.provider.spring.spring7

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import spock.lang.Issue
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class MockMvcTestTargetSpec extends Specification {

    Spring7MockMvcTestTarget mockMvcTestTarget

    def setup() {
        mockMvcTestTarget = new Spring7MockMvcTestTarget(null, [new TestResource()])
    }

    def 'should prepare get request'() {
        given:
        def request = new Request('GET', '/data', [id: ['1234']])
        def interaction = new RequestResponseInteraction('some description', [], request)
        def pact = Mock(Pact)

        when:
        def requestAndClient = mockMvcTestTarget.prepareRequest(pact, interaction, [:])
        def requestBuilder = requestAndClient.first
        def client = requestAndClient.second

        then:
        client instanceof MockMvc
        def builtRequest = requestBuilder.buildRequest(null)
        builtRequest.requestURI == '/data'
        builtRequest.method == 'GET'
        builtRequest.parameterMap.id[0] == '1234'
    }

    def 'should prepare get request with custom mockMvc'() {
        given:
        def mockMvc = MockMvcBuilders.standaloneSetup(new TestResource()).build()
        def mockMvcTestTarget = new Spring7MockMvcTestTarget(mockMvc)
        def request = new Request('GET', '/data', [id: ['1234']])
        def interaction = new RequestResponseInteraction('some description', [], request)
        def pact = Mock(Pact)

        when:
        def requestAndClient = mockMvcTestTarget.prepareRequest(pact, interaction, [:])
        def requestBuilder = requestAndClient.first
        def client = requestAndClient.second

        then:
        client === mockMvc
        def builtRequest = requestBuilder.buildRequest(null)
        builtRequest.requestURI == '/data'
        builtRequest.method == 'GET'
        builtRequest.parameterMap.id[0] == '1234'
    }

    def 'should prepare post request'() {
        given:
        def request = new Request('POST', '/data', [id: ['1234']], [:],
                OptionalBody.body('{"foo":"bar"}'.getBytes(StandardCharsets.UTF_8)))
        def interaction = new RequestResponseInteraction('some description', [], request)
        def pact = Mock(Pact)

        when:
        def requestAndClient = mockMvcTestTarget.prepareRequest(pact, interaction, [:])
        def requestBuilder = requestAndClient.first
        def client = requestAndClient.second

        then:
        client instanceof MockMvc
        def builtRequest = requestBuilder.characterEncoding('UTF-8').buildRequest(null)
        builtRequest.requestURI == '/data'
        builtRequest.contentAsString == '{"foo":"bar"}'
        builtRequest.method == 'POST'
        builtRequest.parameterMap.id[0] == '1234'
    }

    def 'should execute interaction'() {
        given:
        def request = new Request('GET', '/data', [id: ['1234']])
        def interaction = new RequestResponseInteraction('some description', [], request)
        def pact = Mock(Pact)
        def requestAndClient = mockMvcTestTarget.prepareRequest(pact, interaction, [:])
        def requestBuilder = requestAndClient.first
        def client = requestAndClient.second

        when:
        def response = mockMvcTestTarget.executeInteraction(client, requestBuilder)

        then:
        response.statusCode == 200
        response.contentType.toString() == 'application/json'
        response.body.valueAsString() == 'Hello 1234'
    }

    def 'should execute interaction with custom mockMvc'() {
        given:
        def mockMvc = MockMvcBuilders.standaloneSetup(new TestResource()).build()
        def mockMvcTestTarget = new Spring7MockMvcTestTarget(mockMvc)

        def request = new Request('GET', '/data', [id: ['1234']])
        def interaction = new RequestResponseInteraction('some description', [], request)
        def pact = Mock(Pact)
        def requestAndClient = mockMvcTestTarget.prepareRequest(pact, interaction, [:])
        def requestBuilder = requestAndClient.first
        def client = requestAndClient.second

        when:
        def responseMap = mockMvcTestTarget.executeInteraction(client, requestBuilder)

        then:
        responseMap.statusCode == 200
        responseMap.contentType.toString() == 'application/json'
        responseMap.body.valueAsString() == 'Hello 1234'
    }

    @Issue('#1788')
    def 'query parameters with null and empty values'() {
        given:
        def pactRequest = new Request('GET', '/', ['A': ['', ''], 'B': [null, null]])

        when:
        def request = mockMvcTestTarget.requestUriString(pactRequest)

        then:
        request.query == 'A=&A=&B&B'
    }

    def 'should prepare multipart file upload request'() {
        given:
        def request = new Request('POST', '/upload')
                .withMultipartFileUpload('file', 'filename', 'text/csv', 'file,contents')
        def interaction = new RequestResponseInteraction('multipart upload', [], request)
        def pact = Mock(Pact)

        when:
        def requestAndClient = mockMvcTestTarget.prepareRequest(pact, interaction, [:])
        def requestBuilder = requestAndClient.first
        def client = requestAndClient.second

        then:
        client instanceof MockMvc
        def builtRequest = requestBuilder.buildRequest(null)
        builtRequest.requestURI == '/upload'
        builtRequest.method == 'POST'
        builtRequest.contentType.startsWith('multipart/')
    }

    def 'should execute interaction with multipart file upload'() {
        given:
        def request = new Request('POST', '/upload')
                .withMultipartFileUpload('file', 'filename', 'text/csv', 'file,contents')
        def interaction = new RequestResponseInteraction('multipart upload', [], request)
        def pact = Mock(Pact)
        def requestAndClient = mockMvcTestTarget.prepareRequest(pact, interaction, [:])
        def requestBuilder = requestAndClient.first
        def client = requestAndClient.second

        when:
        def response = mockMvcTestTarget.executeInteraction(client, requestBuilder)

        then:
        response.statusCode == 200
        response.body.valueAsString() == 'file|text/csv|filename|file,contents'
    }

    @RestController
    static class TestResource {
        @GetMapping(value = '/data', produces = 'application/json')
        @ResponseStatus(HttpStatus.OK)
        String getData(@RequestParam('id') String id) {
            "Hello $id"
        }

        @PostMapping(value = '/upload', produces = 'text/plain')
        @ResponseStatus(HttpStatus.OK)
        String upload(@RequestParam('file') MultipartFile file) {
            [file.name, file.contentType, file.originalFilename, file.inputStream.text].join('|')
        }
    }
}
