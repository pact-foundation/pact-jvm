package au.com.dius.pact.model

import au.com.dius.pact.model.v3.messaging.MessagePact
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.AmazonS3URI
import com.github.zafarkhaja.semver.Version
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import kotlin.Pair

import static au.com.dius.pact.model.PactReaderKt.newHttpClient

/**
 * Class to load a Pact from a JSON source using a version strategy
 */
@Slf4j
@SuppressWarnings('DuplicateStringLiteral')
class PactReader {

  private static final String CLASSPATH_URI_START = 'classpath:'

  /**
   * Loads a pact file from either a File or a URL
   * @param source a File or a URL
   */
  static Pact<? extends Interaction> loadPact(Map options = [:], def source) {
    Pair<Object, PactSource> pactInfo = loadFile(source, options)
    def version = determineSpecVersion(pactInfo.first)
    def specVersion = Version.valueOf(version)
    switch (specVersion.majorVersion) {
        case 3:
            return loadV3Pact(pactInfo.second, pactInfo.first)
        default:
            return loadV2Pact(pactInfo.second, pactInfo.first)
    }
  }

  static String determineSpecVersion(def pactInfo) {
    def version = '2.0.0'
    if (pactInfo.metadata) {
      def metadata = pactInfo.metadata as Map
      if (metadata.containsKey('pactSpecificationVersion')) {
        version = metadata['pactSpecificationVersion']
      } else if (metadata.containsKey('pactSpecification')) {
        version = specVersion(metadata['pactSpecification'], version)
      } else if (metadata.containsKey('pact-specification')) {
        version = specVersion(metadata['pact-specification'], version)
      }
    }

    if (version == '3.0') {
      version = '3.0.0'
    }

    version
  }

  private static String specVersion(def specification, String defaultVersion) {
    if (specification instanceof Map && specification.containsKey('version') &&
            specification['version'] instanceof String) {
      return specification['version']
    }

    defaultVersion
  }

  @SuppressWarnings('UnusedMethodParameter')
  static Pact<? extends Interaction> loadV3Pact(def source, def pactJson) {
      if (pactJson.messages) {
        def pact = MessagePact.fromMap(pactJson)
        pact.source = source
        pact
      } else {
        def transformedJson = transformJson(pactJson)
        def provider = Provider.fromMap(transformedJson.provider as Map)
        def consumer = Consumer.fromMap(transformedJson.consumer as Map)

        def interactions = transformedJson.interactions.collect { i ->
          def request = extractRequest(i.request)
          def response = extractResponse(i.response)
          def providerStates = []
          if (i.providerStates) {
            providerStates = i.providerStates.collect { ProviderState.fromMap(it) }
          } else if (i.providerState) {
            providerStates << new ProviderState(i.providerState)
          }
          new RequestResponseInteraction(i.description, providerStates, request, response, i['_id'])
        }

        def pact = new RequestResponsePact(provider, consumer, interactions)
        pact.source = source
        pact
      }
  }

  @SuppressWarnings('UnusedMethodParameter')
  static Pact<? extends Interaction> loadV2Pact(def source, def pactJson) {
    def transformedJson = transformJson(pactJson)
    def provider = Provider.fromMap(transformedJson.provider ?: [:])
    def consumer = Consumer.fromMap(transformedJson.consumer ?: [:])

    def interactions = transformedJson.interactions.collect { i ->
      def request = extractRequest(i.request ?: [:])
      def response = extractResponse(i.response ?: [:])
      new RequestResponseInteraction(i.description, i.providerState ? [ new ProviderState(i.providerState) ] : [],
        request, response, i['_id'])
    }

    def pact = new RequestResponsePact(provider, consumer, interactions)
    pact.source = source
    pact
  }

  static Response extractResponse(responseJson) {
    extractBody(responseJson)
    Response.fromMap(responseJson)
  }

  static Request extractRequest(requestJson) {
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

  @CompileStatic
  private static Pair<Object, PactSource> loadFile(def source, Map options = [:]) {
    if (source instanceof ClosurePactSource) {
      loadFile(source.closure.get(), options)
    } else {
      if (source instanceof FileSource) {
        new Pair(new JsonSlurper().parse(source.file), source)
      } else if (source instanceof InputStream || source instanceof Reader || source instanceof File) {
        loadPactFromFile(source)
      } else if (source instanceof BrokerUrlSource) {
        PactReaderKt.loadPactFromUrl(source, options, null)
      } else if (source instanceof URL || source instanceof UrlPactSource) {
        UrlPactSource urlSource = source instanceof URL ? new UrlSource(source.toString()) : source as UrlPactSource
        PactReaderKt.loadPactFromUrl(urlSource as UrlPactSource, options, newHttpClient(urlSource.url, options))
      } else if (source instanceof String && source.toLowerCase() ==~ '(https?|file)://?.*') {
        def urlSource = new UrlSource(source)
        PactReaderKt.loadPactFromUrl(urlSource, options, newHttpClient(urlSource.url, options))
      } else if (source instanceof String && source.toLowerCase() ==~ 's3://.*') {
        loadPactFromS3Bucket(source, options)
      } else if (source instanceof String && source.startsWith(CLASSPATH_URI_START)) {
        loadPactFromClasspath((source as String) - CLASSPATH_URI_START)
      }  else if (source instanceof String && fileExists(source)) {
        def file = source as File
        new Pair(new JsonSlurper().parse(file), new FileSource(file))
      } else {
        try {
          new Pair(new JsonSlurper().parseText(source.toString()), UnknownPactSource.INSTANCE)
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

  private static Pair<Object, PactSource> loadPactFromClasspath(String source) {
    InputStream inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(source)
    if (inputStream == null) {
      throw new IllegalStateException("not found on classpath: $source")
    }
    inputStream.withCloseable { loadPactFromFile(it) }
  }

  private static boolean fileExists(String path) {
      new File(path).exists()
  }

  private static s3Client() {
    new AmazonS3Client()
  }
}
