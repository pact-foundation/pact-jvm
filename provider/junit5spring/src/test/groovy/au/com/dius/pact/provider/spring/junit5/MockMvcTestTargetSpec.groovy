package au.com.dius.pact.provider.spring.junit5

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class MockMvcTestTargetSpec extends Specification {

    MockMvcTestTarget mockMvcTestTarget

    def setup() {
        mockMvcTestTarget = new MockMvcTestTarget(null, [new TestResource()])
    }

    def 'should prepare get request'() {
        given:
        def request = new Request('GET', '/data', [id: ['1234']])
        def interaction = new RequestResponseInteraction('some description', [], request)
        when:
        def requestAndClient = mockMvcTestTarget.prepareRequest(interaction, [:])
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
        def mockMvcTestTarget = new MockMvcTestTarget(mockMvc)
        def request = new Request('GET', '/data', [id: ['1234']])
        def interaction = new RequestResponseInteraction('some description', [], request)
        when:
        def requestAndClient = mockMvcTestTarget.prepareRequest(interaction, [:])
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
        when:
        def requestAndClient = mockMvcTestTarget.prepareRequest(interaction, [:])
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
        def requestAndClient = mockMvcTestTarget.prepareRequest(interaction, [:])
        def requestBuilder = requestAndClient.first
        def client = requestAndClient.second
        when:
        def response = mockMvcTestTarget.executeInteraction(client, requestBuilder)
        then:
        response.statusCode == 200
        response.contentType.toString() == 'application/json'
        response.body == 'Hello 1234'
    }

    def 'should execute interaction with custom mockMvc'() {
        given:
        def mockMvc = MockMvcBuilders.standaloneSetup(new TestResource()).build()
        def mockMvcTestTarget = new MockMvcTestTarget(mockMvc)

        def request = new Request('GET', '/data', [id: ['1234']])
        def interaction = new RequestResponseInteraction('some description', [], request)
        def requestAndClient = mockMvcTestTarget.prepareRequest(interaction, [:])
        def requestBuilder = requestAndClient.first
        def client = requestAndClient.second
        when:
        def responseMap = mockMvcTestTarget.executeInteraction(client, requestBuilder)
        then:
        responseMap.statusCode == 200
        responseMap.contentType.toString() == 'application/json'
        responseMap.body == 'Hello 1234'
    }

    @RestController
    static class TestResource {
        @GetMapping(value = '/data', produces = 'application/json')
        @ResponseStatus(HttpStatus.OK)
        String getData(@RequestParam('id') String id) {
            "Hello $id"
        }
    }
}
