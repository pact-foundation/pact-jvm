package steps.v1

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.HeaderParser
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import groovy.transform.Canonical
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given

import static au.com.dius.pact.core.model.PactReaderKt.queryStringToMap
import static io.ktor.http.HttpHeaderValueParserKt.parseHeaderValue

@Canonical
class CompatibilitySuiteWorld {
  List<RequestResponseInteraction> interactions = []
}

class SharedSteps {
  CompatibilitySuiteWorld world

  SharedSteps(CompatibilitySuiteWorld world) {
    this.world = world
  }

  @Given('the following HTTP interactions have been defined:')
  @SuppressWarnings('AbcMetric')
  void the_following_http_interactions_have_been_setup(DataTable dataTable) {
    dataTable.entries().eachWithIndex { Map<String, String> entry, int i ->
      Interaction interaction = new RequestResponseInteraction("Interaction $i", [], new Request(),
        new Response(), "ID$i")

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
        }.collectEntries {
          Map.entry(it[0].trim(), parseHeaderValue(it[1].trim()).collect { HeaderParser.INSTANCE.hvToString(it) })
        }
      }

      if (entry['body']) {
        if (entry['body'].startsWith('JSON:')) {
          interaction.request.headers['content-type'] = ['application/json']
          interaction.request.body = OptionalBody.body(entry['body'][5..-1].bytes, new ContentType('application/json'))
        } else if (entry['body'].startsWith('XML:')) {
          interaction.request.headers['content-type'] = ['application/xml']
          interaction.request.body = OptionalBody.body(entry['body'][4..-1].bytes, new ContentType('application/xml'))
        } else {
          String contentType = 'text/plain'
          if (entry['body'].endsWith('.json')) {
            contentType = 'application/json'
          } else if (entry['body'].endsWith('.xml')) {
            contentType = 'application/xml'
          }
          interaction.request.headers['content-type'] = [contentType]
          File contents = new File("pact-compatibility-suite/fixtures/${entry['body']}")
          contents.withInputStream {
            interaction.request.body = OptionalBody.body(it.readAllBytes(), new ContentType(contentType))
          }
        }
      }

      if (entry['response']) {
        interaction.response.status = entry['response'].toInteger()
      }

      if (entry['response body']) {
        String contentType = 'text/plain'
        if (entry['response content']) {
          contentType = entry['response content']
        }
        interaction.response.headers['content-type'] = [ contentType ]
        File contents = new File("pact-compatibility-suite/fixtures/${entry['response body']}")
        contents.withInputStream {
          interaction.response.body = OptionalBody.body(it.readAllBytes(), new ContentType(contentType))
        }
      }

      world.interactions << interaction
    }
  }
}
