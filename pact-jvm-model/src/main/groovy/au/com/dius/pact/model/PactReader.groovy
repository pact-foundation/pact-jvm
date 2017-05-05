package au.com.dius.pact.model

import au.com.dius.pact.model.v3.messaging.MessagePact
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.AmazonS3URI
import com.github.zafarkhaja.semver.Version
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovyx.net.http.RESTClient

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
  static Pact loadPact(Map options = [:], def source) {
    def pact = loadFile(source, options)
    def version = '2.0.0'
    def specification = pact.metadata?.'pact-specification'
    if (specification instanceof Map && specification.version) {
      version = specification.version
    }
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
  static Pact loadV3Pact(def source, def pactJson) {
      if (pactJson.messages) {
          MessagePact.fromMap(pactJson)
      } else {
        def transformedJson = transformJson(pactJson)
        def provider = transformedJson.provider as Provider
        def consumer = transformedJson.consumer as Consumer

        def interactions = transformedJson.interactions.collect { i ->
          def request = extractRequestV3(i.request)
          def response = extractResponse(i.response)
          new RequestResponseInteraction(i.description, i.providerState, request, response)
        }

        new RequestResponsePact(provider, consumer, interactions)
      }
  }

  @SuppressWarnings('UnusedMethodParameter')
  static Pact loadV2Pact(def source, def pactJson) {
    def transformedJson = transformJson(pactJson)
    def provider = transformedJson.provider as Provider
    def consumer = transformedJson.consumer as Consumer

    def interactions = transformedJson.interactions.collect { i ->
      def request = extractRequestV2(i.request ?: [:])
      def response = extractResponse(i.response ?: [:])
      new RequestResponseInteraction(i.description, i.providerState, request, response)
    }

    new RequestResponsePact(provider, consumer, interactions)
  }

  static Response extractResponse(responseJson) {
    extractBody(responseJson)
    Response.fromMap(responseJson)
  }

  static Request extractRequestV2(requestJson) {
    extractBody(requestJson)
    requestJson.query = queryStringToMap(requestJson.query)
    Request.fromMap(requestJson)
  }

  @SuppressWarnings('DuplicateStringLiteral')
  static Map<String, List<String>> queryStringToMap(String query, boolean decode = true) {
    if (query) {
      query.split('&')*.split('=', 2).inject([:]) { Map map, String[] nameAndValue ->
        def name = decode ? URLDecoder.decode(nameAndValue.first(), 'UTF-8') : nameAndValue.first()
        def value = decode ? URLDecoder.decode(nameAndValue.last(), 'UTF-8') : nameAndValue.last()
        if (map.containsKey(name)) {
          map[name] << value
        } else {
          map[name] = [value]
        }
        map
      }
    } else {
      [:]
    }
  }

  static Request extractRequestV3(requestJson) {
    extractBody(requestJson)
    Request.fromMap(requestJson)
  }

  static void extractBody(json) {
    if (json.containsKey('body') && json.body != null && !(json.body instanceof String)) {
      json.body = JsonOutput.toJson(json.body)
    }
  }

  @SuppressWarnings('DuplicateStringLiteral')
  static transformJson(def pactJson) {
    pactJson.interactions = pactJson.interactions*.collectEntries { k, v ->
      def entry = [k, v]
      switch (k) {
        case 'provider_state':
          entry = ['providerState', v]
          break
        case 'request':
          entry = ['request', transformRequestResponseJson(v)]
          break
        case 'response':
          entry = ['response', transformRequestResponseJson(v)]
          break
      }
      entry
    }
    pactJson
  }

  @SuppressWarnings('DuplicateStringLiteral')
  static transformRequestResponseJson(def requestJson) {
    requestJson.collectEntries { k, v ->
      def entry = [k, v]
      switch (k) {
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
      entry
    }
  }

  private static loadFile(def source, Map options = [:]) {
      if (source instanceof InputStream || source instanceof Reader || source instanceof File) {
          new JsonSlurper().parse(source)
      } else if (source instanceof URL) {
        loadPactFromUrl(source, options)
      } else if (source instanceof String && source.toLowerCase() ==~ '(https?|file)://.*') {
        loadPactFromUrl(new URL(source), options)
      } else if (source instanceof String && source.toLowerCase() ==~ 's3://.*') {
        loadPactFromS3Bucket(source, options)
      } else if (source instanceof String && fileExists(source)) {
          new JsonSlurper().parse(source as File)
      } else {
          try {
            new JsonSlurper().parseText(source)
          } catch (e) {
            throw new UnsupportedOperationException(
              "Unable to load pact file from '$source' as it is neither a json document, file, input stream, " +
                'reader or an URL',
              e)
          }
      }
  }

  @SuppressWarnings('UnusedPrivateMethodParameter')
  private static loadPactFromS3Bucket(String source, Map options) {
    def s3Uri = new AmazonS3URI(source)
    def client = s3Client()
    def s3Pact = client.getObject(s3Uri.bucket, s3Uri.key)
    new JsonSlurper().parse(s3Pact.objectContent)
  }

  @SuppressWarnings('DuplicateNumberLiteral')
  private static loadPactFromUrl(URL source, Map options) {
    if (options.authentication) {
      def http = newHttpClient(source)
      switch (options.authentication.first().toLowerCase()) {
        case 'basic':
          if (options.authentication.size() > 2) {
            http.auth.basic(options.authentication[1].toString(), options.authentication[2].toString())
          } else {
            log.warn('Basic authentication requires a username and password, ignoring.')
          }
          break
      }
      http.get(headers: [Accept: JSON]).data
    } else {
      new JsonSlurper().parse(source, ACCEPT_JSON)
    }
  }

  private static newHttpClient(URL source) {
    new RESTClient(source)
  }

  private static boolean fileExists(String path) {
      new File(path).exists()
  }

  private static s3Client() {
    new AmazonS3Client()
  }
}
