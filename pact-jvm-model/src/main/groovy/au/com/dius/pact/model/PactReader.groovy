package au.com.dius.pact.model

import au.com.dius.pact.model.v3.messaging.MessagePact
import com.github.zafarkhaja.semver.Version
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

/**
 * Class to load a Pact from a JSON source using a version strategy
 */
@Slf4j
class PactReader {

  private static final String JSON = 'application/json'
  private static final Map ACCEPT_JSON = [requestProperties: [Accept: JSON]]
  /**
   * Loads a pact file from either a File or a URL
   * @param source a File or a URL
   */
  static BasePact loadPact(def source) {
      def pact = loadFile(source)
      def version = pact.metadata?.'pact-specification'?.version ?: '2.0.0'
      if (version == '3.0') {
          version = '3.0.0'
      }
      def specVersion = Version.valueOf(version)
      switch (specVersion.majorVersion) {
          case 3:
              return loadV3Pact(source, pact)
          default:
              return loadV2Pact(source, pact)
      }
  }

  @SuppressWarnings('UnusedMethodParameter')
  static BasePact loadV3Pact(def source, def pactJson) {
      if (pactJson.messages) {
          new MessagePact().fromMap(pactJson)
      } else {
        def transformedJson = recursiveTransformJson(pactJson)
        def provider = transformedJson.provider as Provider
        def consumer = transformedJson.consumer as Consumer

        def interactions = transformedJson.interactions.collect { i ->
          def request = extractRequestV3(i.request)
          def response = extractResponse(i.response)
          new Interaction(i.description, i.providerState, request, response)
        }

        new Pact(provider, consumer, interactions)
      }
  }

  @SuppressWarnings('UnusedMethodParameter')
  static BasePact loadV2Pact(def source, def pactJson) {
    def transformedJson = recursiveTransformJson(pactJson)
    def provider = transformedJson.provider as Provider
    def consumer = transformedJson.consumer as Consumer

    def interactions = transformedJson.interactions.collect { i ->
      def request = extractRequestV2(i.request ?: [:])
      def response = extractResponse(i.response ?: [:])
      new Interaction(i.description, i.providerState, request, response)
    }

    new Pact(provider, consumer, interactions)
  }

  static Response extractResponse(responseJson) {
    responseJson.body = extractBody(responseJson.body)
    Response.fromMap(responseJson)
  }

  static Request extractRequestV2(requestJson) {
    requestJson.body = extractBody(requestJson.body)
    requestJson.query = queryStringToMap(requestJson.query)
    Request.fromMap(requestJson)
  }

  @SuppressWarnings('DuplicateStringLiteral')
  static Map<String, List<String>> queryStringToMap(String query, boolean decode = true) {
    if (query) {
      query.split('&')*.split('=').inject([:]) { Map map, String[] nameAndValue ->
        def name = decode ? URLDecoder.decode(nameAndValue.first(), 'UTF-8') : nameAndValue.first()
        def value = decode ? URLDecoder.decode(nameAndValue.last(), 'UTF-8') : nameAndValue.last()
        if (map.containsKey(name)) {
          map[name] << value
        } else {
          map[name] = [value]
        }
        map
      }
    }
  }

  static Request extractRequestV3(requestJson) {
    requestJson.body = extractBody(requestJson.body)
    Request.fromMap(requestJson)
  }

  static extractBody(body) {
    if (body == null || body instanceof String) {
      body
    } else {
      JsonOutput.toJson(body)
    }
  }

  @SuppressWarnings('DuplicateStringLiteral')
  static recursiveTransformJson(def pactJson) {
    pactJson.collectEntries { k, v ->
      def entry = [k, v]
      switch (k) {
        case 'provider_state':
          entry = ['providerState', v]
            break
        case 'responseMatchingRules':
          entry = ['matchingRules', v]
            break
        case 'requestMatchingRules':
          entry = ['matchingRules', v]
            break
        case 'method':
          entry = ['method', v ? v.toString().toUpperCase() : v]
            break
      }

      if (v instanceof Map) {
        entry[1] = recursiveTransformJson(v)
      } else if (v instanceof Collection) {
        entry[1] = v.collect {
          if (it instanceof Map) {
            recursiveTransformJson(it)
          } else {
            it
          }
        }
      }

      entry
    }
  }

  private static loadFile(def source) {
      if (source instanceof InputStream || source instanceof Reader || source instanceof File) {
          new JsonSlurper().parse(source)
      } else if (source instanceof URL) {
          new JsonSlurper().parse(source, ACCEPT_JSON)
      } else if (source instanceof String && source ==~ '(https?|file)://.*') {
          new JsonSlurper().parse(new URL(source), ACCEPT_JSON)
      } else if (source instanceof String && fileExists(source)) {
          new JsonSlurper().parse(source as File)
      } else {
          throw new UnsupportedOperationException(
                  "Unable to load pact file from '$source' as it is neither a file, input stream, reader or an URL")
      }
  }

  private static boolean fileExists(String path) {
      new File(path).exists()
  }
}
