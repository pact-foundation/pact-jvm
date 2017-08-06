package au.com.dius.pact.model

import au.com.dius.pact.model.v3.messaging.MessagePact
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.AmazonS3URI
import com.github.zafarkhaja.semver.Version
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovyx.net.http.RESTClient
import kotlin.Pair

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
    Pair<Object, PactSource> pactInfo = loadFile(source, options)
    def version = '2.0.0'
    def specification = pactInfo.first.metadata?.'pact-specification'
    if (specification instanceof Map && specification.version) {
      version = specification.version
    }
    if (version == '3.0') {
        version = '3.0.0'
    }
    def specVersion = Version.valueOf(version)
    switch (specVersion.majorVersion) {
        case 3:
            return loadV3Pact(pactInfo.second, pactInfo.first)
        default:
            return loadV2Pact(pactInfo.second, pactInfo.first)
    }
  }

  @SuppressWarnings('UnusedMethodParameter')
  static Pact loadV3Pact(def source, def pactJson) {
      if (pactJson.messages) {
        def pact = MessagePact.fromMap(pactJson)
        pact.source = source
        pact
      } else {
        def transformedJson = transformJson(pactJson)
        def provider = Provider.fromMap(transformedJson.provider as Map)
        def consumer = Consumer.fromMap(transformedJson.consumer as Map)

        def interactions = transformedJson.interactions.collect { i ->
          def request = extractRequestV3(i.request)
          def response = extractResponse(i.response)
          def providerStates = []
          if (i.providerStates) {
            providerStates = i.providerStates.collect { ProviderState.fromMap(it) }
          } else if (i.providerState) {
            providerStates << new ProviderState(i.providerState)
          }
          new RequestResponseInteraction(i.description, providerStates, request, response)
        }

        def pact = new RequestResponsePact(provider, consumer, interactions)
        pact.source = source
        pact
      }
  }

  @SuppressWarnings('UnusedMethodParameter')
  static Pact loadV2Pact(def source, def pactJson) {
    def transformedJson = transformJson(pactJson)
    def provider = Provider.fromMap(transformedJson.provider ?: [:])
    def consumer = Consumer.fromMap(transformedJson.consumer ?: [:])

    def interactions = transformedJson.interactions.collect { i ->
      def request = extractRequestV2(i.request ?: [:])
      def response = extractResponse(i.response ?: [:])
      new RequestResponseInteraction(i.description, i.providerState ? [ new ProviderState(i.providerState) ] : [],
        request, response)
    }

    def pact = new RequestResponsePact(provider, consumer, interactions)
    pact.source = source
    pact
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
    pactJson.interactions = pactJson.interactions.collect { i ->
      def interaction = i.collectEntries { k, v ->
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
      if (i.providerState) {
        interaction.providerState = i.providerState
      }
      interaction
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

  private static Pair<Object, PactSource> loadFile(def source, Map options = [:]) {
    if (source instanceof ClosurePactSource) {
      loadFile(source.closure.get(), options)
    } else {
      if (source instanceof FileSource) {
        new Pair(new JsonSlurper().parse(source.file), source)
      } else if (source instanceof InputStream || source instanceof Reader || source instanceof File) {
        loadPactFromFile(source)
      } else if (source instanceof URL || source instanceof UrlPactSource) {
        loadPactFromUrl(source instanceof URL ? new UrlSource(source.toString()) : source, options)
      } else if (source instanceof String && source.toLowerCase() ==~ '(https?|file)://?.*') {
        loadPactFromUrl(new UrlSource(source), options)
      } else if (source instanceof String && source.toLowerCase() ==~ 's3://.*') {
        loadPactFromS3Bucket(source, options)
      } else if (source instanceof String && fileExists(source)) {
        def file = source as File
        new Pair(new JsonSlurper().parse(file), new FileSource(file))
      } else {
        try {
          new Pair(new JsonSlurper().parseText(source), UnknownPactSource.INSTANCE)
        } catch (e) {
          throw new UnsupportedOperationException(
            "Unable to load pact file from '$source' as it is neither a json document, file, input stream, " +
              'reader or an URL',
            e)
        }
      }
    }
  }

  static Pair<Object, PactSource> loadPactFromFile(def source) {
    def pactData = new JsonSlurper().parse(source)
    if (source instanceof InputStream) {
      new Pair(pactData, InputStreamPactSource.INSTANCE)
    } else if (source instanceof Reader) {
      new Pair(pactData, ReaderPactSource.INSTANCE)
    } else if (source instanceof File) {
      new Pair(pactData, new FileSource(source))
    } else {
      new Pair(pactData, UnknownPactSource.INSTANCE)
    }
  }

  @SuppressWarnings('UnusedPrivateMethodParameter')
  private static Pair<Object, PactSource> loadPactFromS3Bucket(String source, Map options) {
    def s3Uri = new AmazonS3URI(source)
    def client = s3Client()
    def s3Pact = client.getObject(s3Uri.bucket, s3Uri.key)
    new Pair(new JsonSlurper().parse(s3Pact.objectContent), new S3PactSource(source))
  }

  @SuppressWarnings('DuplicateNumberLiteral')
  private static Pair<Object, PactSource> loadPactFromUrl(UrlPactSource source, Map options) {
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
      new Pair(http.get(headers: [Accept: JSON]).data, source)
    } else {
      new Pair(new JsonSlurper().parse(new URL(source.url), ACCEPT_JSON), source)
    }
  }

  private static newHttpClient(UrlPactSource source) {
    new RESTClient(source.url)
  }

  private static boolean fileExists(String path) {
      new File(path).exists()
  }

  private static s3Client() {
    new AmazonS3Client()
  }
}
