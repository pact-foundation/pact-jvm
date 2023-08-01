package steps.v2

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.HeaderParser
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponsePact
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.When
import steps.shared.CompatibilitySuiteWorld
import steps.shared.MockServerData

import static au.com.dius.pact.consumer.MockHttpServerKt.mockServer
import static au.com.dius.pact.core.model.PactReaderKt.queryStringToMap
import static io.ktor.http.HttpHeaderValueParserKt.parseHeaderValue
import static steps.shared.SharedSteps.configureBody
import static steps.shared.SharedSteps.determineContentType

class HttpConsumer {
  CompatibilitySuiteWorld world
  MockServerData mockServerData

  HttpConsumer(CompatibilitySuiteWorld world, MockServerData mockServerData) {
    this.mockServerData = mockServerData
    this.world = world
  }

  @When('the mock server is started with interaction {int} but with the following changes:')
  void the_mock_server_is_started_with_interaction_but_with_the_following_changes(Integer num, DataTable dataTable) {
    def interaction = world.interactions[num - 1]
    def entry = dataTable.entries().first()
    if (entry['method']) {
      interaction.request.method = entry['method']
    }

    if (entry['path']) {
      interaction.request.path = entry['path']
    }

    if (entry['query']) {
      interaction.request.query = queryStringToMap(entry['query'])
    }

    if (entry['headers']) {
      interaction.request.headers = entry['headers'].split(',').collect {
        it.trim()[1..-2].split(':')
      }.collect {
        [it[0].trim(), parseHeaderValue(it[1].trim()).collect { HeaderParser.INSTANCE.hvToString(it) }]
      }.inject([:]) { acc, e ->
        if (acc.containsKey(e[0])) {
          acc[e[0]] += e[1].flatten()
        } else {
          acc[e[0]] = e[1].flatten()
        }
        acc
      }
    }

    if (entry['body']) {
      def part = configureBody(entry['body'], determineContentType(entry['body'],
        interaction.request.contentTypeHeader()))
      interaction.request.body = part.body
      interaction.request.headers.putAll(part.headers)
    }

    mockServerData.pact = new RequestResponsePact(new Provider('p'),
      new Consumer('v1-compatibility-suite-c'), [interaction])
    mockServerData.config = new MockProviderConfig()
    mockServerData.mockServer = mockServer(mockServerData.pact, mockServerData.config)
    mockServerData.mockServer.start()
  }
}
