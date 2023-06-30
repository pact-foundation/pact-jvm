package steps.shared

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.HeaderParser
import au.com.dius.pact.core.model.HttpPart
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import groovy.transform.Canonical
import groovy.xml.XmlSlurper
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given

import static au.com.dius.pact.core.model.PactReaderKt.queryStringToMap
import static io.ktor.http.HttpHeaderValueParserKt.parseHeaderValue

@Canonical
class CompatibilitySuiteWorld {
  List<RequestResponseInteraction> interactions = []
}

@SuppressWarnings('MethodSize')
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
        configureBody(entry['body'], interaction.request)
      }

      if (entry['matching rules']) {
        JsonValue json
        if (entry['matching rules'].startsWith('JSON:')) {
          json = JsonParser.INSTANCE.parseString(entry['body'][5..-1])
        } else {
          File contents = new File("pact-compatibility-suite/fixtures/${entry['matching rules']}")
          contents.withInputStream {
            json = JsonParser.INSTANCE.parseStream(it)
          }
        }
        interaction.request.matchingRules = MatchingRulesImpl.fromJson(json)
      }

      if (entry['response']) {
        interaction.response.status = entry['response'].toInteger()
      }

      if (entry['response headers']) {
        interaction.response.headers = entry['response headers'].split(',').collect {
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

      if (entry['response body']) {
        configureBody(entry['response body'], interaction.response)
      }

      if (entry['response matching rules']) {
        JsonValue json
        if (entry['response matching rules'].startsWith('JSON:')) {
          json = JsonParser.INSTANCE.parseString(entry['response matching rules'][5..-1])
        } else {
          File contents = new File("pact-compatibility-suite/fixtures/${entry['response matching rules']}")
          contents.withInputStream {
            json = JsonParser.INSTANCE.parseStream(it)
          }
        }
        interaction.response.matchingRules = MatchingRulesImpl.fromJson(json)
      }

      world.interactions << interaction
    }
  }

  static void configureBody(String entry, HttpPart part) {
    if (entry.startsWith('JSON:')) {
      part.headers['content-type'] = ['application/json']
      part.body = OptionalBody.body(entry[5..-1].bytes, new ContentType('application/json'))
    } else if (entry.startsWith('XML:')) {
      part.headers['content-type'] = ['application/xml']
      part.body = OptionalBody.body(entry[4..-1].trim().bytes, new ContentType('application/xml'))
    } else if (entry.startsWith('file:')) {
      if (entry.endsWith('-body.xml')) {
        File contents = new File("pact-compatibility-suite/fixtures/${entry[5..-1].trim()}")
        def fixture = new XmlSlurper().parse(contents)
        def contentType = fixture.contentType.toString()
        part.headers['content-type'] = [contentType]
        part.body = OptionalBody.body(fixture.contents.text(), new ContentType(contentType))
      } else {
        String contentType = determineContentType(entry, part)
        part.headers['content-type'] = [contentType]
        File contents = new File("pact-compatibility-suite/fixtures/${entry[5..-1].trim()}")
        contents.withInputStream {
          part.body = OptionalBody.body(it.readAllBytes(), new ContentType(contentType))
        }
      }
    } else {
      part.headers['content-type'] = [determineContentType(entry, part)]
      part.body = OptionalBody.body(entry)
    }
  }

  private static String determineContentType(String entry, HttpPart part) {
    String contentType = part.contentTypeHeader()
    if (entry.endsWith('.json')) {
      contentType = 'application/json'
    } else if (entry.endsWith('.xml')) {
      contentType = 'application/xml'
    } else if (entry.endsWith('.jpg')) {
      contentType = 'image/jpeg'
    } else if (entry.endsWith('.pdf')) {
      contentType = 'application/pdf'
    }
    contentType ?: 'text/plain'
  }
}
