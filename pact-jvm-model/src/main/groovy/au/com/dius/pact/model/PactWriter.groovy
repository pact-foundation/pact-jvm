package au.com.dius.pact.model

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.util.jar.JarInputStream

/**
 * Class to write out a pact to a file
 */
@Slf4j
class PactWriter {

  /**
   * Writes out the pact to the provided pact file
   * @param pact Pact to write
   * @param writer Writer to write out with
   * @param pactSpecVersion Pact version to use to control writing
   */
  static writePact(Pact pact, PrintWriter writer, PactSpecVersion pactSpecVersion = PactSpecVersion.V2) {
    pact.sortInteractions()
    Map jsonData = toMap(pact, pactSpecVersion)
    writer.print(JsonOutput.prettyPrint(JsonOutput.toJson(jsonData)))
  }

  static Map toMap(Pact pact, PactSpecVersion pactSpecVersion) {
    [
      provider: objectToMap(pact.provider),
      consumer: objectToMap(pact.consumer),
      interactions: pact.interactions.collect { interactionToMap(it, pactSpecVersion) },
      metadata: metaData(pactSpecVersion >= PactSpecVersion.V3 ? '3.0.0' : '2.0.0')
    ]
  }

  static Map interactionToMap(Interaction interaction, PactSpecVersion pactSpecVersion) {
    [
      providerState: interaction.providerState,
      description: interaction.description,
      request: requestToMap(interaction.request, pactSpecVersion),
      response: responseToMap(interaction.response)
    ]
  }

  static Map responseToMap(Response response) {
    def map = [status: response.status]
    if (response.headers) {
      map.headers = response.headers
    }
    if (response.body) {
      map.body = parseBody(response)
    }
    if (response.matchingRules) {
      map.matchingRules = response.matchingRules
    }
    map
  }

  static parseBody(HttpPart httpPart) {
    if (httpPart.jsonBody()) {
      new JsonSlurper().parseText(httpPart.body)
    } else {
      httpPart.body
    }
  }

  static Map requestToMap(Request request, PactSpecVersion pactSpecVersion) {
    def map = [
      method: request.method.toUpperCase(),
      path: request.path
    ]
    if (request.headers) {
      map.headers = request.headers
    }
    if (request.query) {
      map.query = pactSpecVersion >= PactSpecVersion.V3 ? request.query : mapToQueryStr(request.query)
    }
    if (request.body) {
      map.body = parseBody(request)
    }
    if (request.matchingRules) {
      map.matchingRules = request.matchingRules
    }
    map
  }

  static String mapToQueryStr(Map<String, List<String>> query) {
    query.collectMany { k, v -> v.collect { "$k=$it" } }.join('&')
  }

  private static Map metaData(String version) {
    [
      'pact-specification': [version: version],
      'pact-jvm': [version: lookupVersion()]
    ]
  }

  static String lookupVersion() {
    def url = PactWriter.protectionDomain?.codeSource?.location
    if (url != null) {
      def openStream = url.openStream()
      try {
        def jarStream = new JarInputStream(openStream)
        jarStream.manifest?.mainAttributes?.getValue('Implementation-Version') ?: ''
      } catch (e) {
        log.warn('Could not load pact-jvm manifest', e)
        ''
      } finally {
        openStream.close()
      }
    } else {
      ''
    }
  }

  static Map objectToMap(def object) {
    if (object?.respondsTo('toMap')) {
      object.toMap()
    } else {
      convertToMap(object)
    }
  }

  static Map convertToMap(def object) {
    if (object == null) {
      object
    } else {
      object.properties.findAll { it.key != 'class' }.collectEntries { k, v ->
        if (v instanceof Map) {
          [k, convertToMap(v)]
        } else if (v instanceof Collection) {
          [k, v.collect { convertToMap(v) } ]
        } else {
          [k, v]
        }
      }
    }
  }
}
