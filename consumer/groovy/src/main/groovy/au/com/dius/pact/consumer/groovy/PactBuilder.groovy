package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.Headers
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.model.MockServerImplementation
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactReaderKt
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.matchingrules.Category
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.support.expressions.DataType
import groovy.transform.CompileStatic
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder

import java.util.regex.Pattern

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest

/**
 * Builder DSL for Pact tests
 */
@SuppressWarnings('PropertyName')
class PactBuilder extends GroovyBuilder {

  RequestResponsePact pact = new RequestResponsePact(new Provider(), new Consumer())
  Integer port = 0
  RequestResponseInteraction currentInteraction
  List requestData = []
  List responseData = []
  List interactions = []
  List<ProviderState> providerStates = []
  boolean requestState

  /**
   * Defines the service consumer
   * @param consumer consumer name
   */
  PactBuilder serviceConsumer(String consumer) {
    this.pact.consumer = new Consumer(consumer)
    this
  }

  /**
   * Defines the provider the consumer has a pact with
   * @param provider provider name
   */
  PactBuilder hasPactWith(String provider) {
    this.pact.provider = new Provider(provider)
    this
  }

  /**
   * Defines the port the provider will listen on
   * @param port port number
   */
  @SuppressWarnings('ConfusingMethodName')
  PactBuilder port(int port) {
    this.port = port
    this
  }

  /**
   * Defines the provider state the provider needs to be in for the interaction
   * @param providerState provider state description
   */
  PactBuilder given(String providerState) {
    this.providerStates << new ProviderState(providerState)
    this
  }

  /**
   * Defines the provider state the provider needs to be in for the interaction
   * @param providerState provider state description
   * @param params Data parameters for the provider state
   */
  PactBuilder given(String providerState, Map params) {
    this.providerStates << new ProviderState(providerState, params)
    this
  }

  /**
   * Defines the start of an interaction
   * @param requestDescription Description of the interaction. Must be unique.
   */
  PactBuilder uponReceiving(String requestDescription) {
    buildInteractions()
    this.currentInteraction = new RequestResponseInteraction(requestDescription)
    requestState = true
    this
  }

  def buildInteractions() {
    int numInteractions = Math.min(requestData.size(), responseData.size())
    for (int i = 0; i < numInteractions; i++) {
      MatchingRules requestMatchers = requestData[i].matchers
      MatchingRules responseMatchers = responseData[i].matchers
      Generators requestGenerators = requestData[i].generators
      Generators responseGenerators = responseData[i].generators
      Map headers = setupHeaders(requestData[i].headers ?: [:], requestMatchers, requestGenerators)
      Map query = setupQueryParameters(requestData[i].query ?: [:], requestMatchers, requestGenerators)
      Map responseHeaders = setupHeaders(responseData[i].headers ?: [:], responseMatchers, responseGenerators)
      String path = setupPath(requestData[i].path ?: '/', requestMatchers, requestGenerators)
      def requestBody = requestData[i].body instanceof String ? requestData[i].body.bytes : requestData[i].body
      def responseBody = responseData[i].body instanceof String ? responseData[i].body.bytes : responseData[i].body
      interactions << new RequestResponseInteraction(
        this.currentInteraction.description,
        providerStates,
        new Request(requestData[i].method ?: 'get', path, query, headers,
          requestData[i].containsKey(BODY) ? OptionalBody.body(requestBody, contentType(headers)) :
            OptionalBody.missing(),
          requestMatchers, requestGenerators),
        new Response(responseData[i].status ?: 200, responseHeaders,
          responseData[i].containsKey(BODY) ? OptionalBody.body(responseBody,
            contentType(responseHeaders)) : OptionalBody.missing(),
          responseMatchers, responseGenerators), null
      )
    }
    requestData = []
    responseData = []
  }

  au.com.dius.pact.core.model.ContentType contentType(Map<?, ?> headers) {
    def contentTypeHeader = headers.find { it.key.toLowerCase() == 'content-type' }
    if (contentTypeHeader) {
      new au.com.dius.pact.core.model.ContentType(contentTypeHeader.value.first())
    } else {
      au.com.dius.pact.core.model.ContentType.UNKNOWN
    }
  }

  private static Map setupHeaders(Map headers, MatchingRules matchers, Generators generators) {
    headers.collectEntries { key, value ->
      def header = HEADER
      if (value instanceof Matcher) {
        matchers.addCategory(header).addRule(key, value.matcher)
        [key, [value.value]]
      } else if (value instanceof Pattern) {
        def matcher = new RegexpMatcher(value.toString())
        matchers.addCategory(header).addRule(key, matcher.matcher)
        [key, [matcher.value]]
      } else if (value instanceof GeneratedValue) {
        generators.addGenerator(au.com.dius.pact.core.model.generators.Category.HEADER, key,
          new ProviderStateGenerator(value.expression, DataType.STRING))
        [key, [value.exampleValue]]
      } else {
        [key, value instanceof List ? value : [value]]
      }
    }
  }

  private static String setupPath(def path, MatchingRules matchers, Generators generators) {
    def category = 'path'
    if (path instanceof Matcher) {
      matchers.addCategory(category).addRule(path.matcher)
      path.value
    } else if (path instanceof Pattern) {
      def matcher = new RegexpMatcher(path.toString())
      matchers.addCategory(category).addRule(matcher.matcher)
      matcher.value
    } else if (path instanceof GeneratedValue) {
      generators.addGenerator(au.com.dius.pact.core.model.generators.Category.PATH,
        new ProviderStateGenerator(path.expression, DataType.STRING))
      path.exampleValue
    } else {
      path as String
    }
  }

  private static Map setupQueryParameters(Map query, MatchingRules matchers, Generators generators) {
    query.collectEntries { key, value ->
      def category = 'query'
      if (value[0] instanceof Matcher) {
        matchers.addCategory(category).addRule(key, value[0].matcher)
        [key, [value[0].value]]
      } else if (value[0] instanceof Pattern) {
        def matcher = new RegexpMatcher(value[0].toString())
        matchers.addCategory(category).addRule(key, matcher.matcher)
        [key, [matcher.value]]
      } else if (value[0] instanceof GeneratedValue) {
        generators.addGenerator(au.com.dius.pact.core.model.generators.Category.QUERY, key,
          new ProviderStateGenerator(value[0].expression, DataType.STRING))
        [key, [value[0].exampleValue]]
      } else {
        [key, value]
      }
    }
  }

  /**
   * Defines the request attributes (body, headers, etc.)
   * @param requestData Map of attributes
   */
  PactBuilder withAttributes(Map requestData) {
    def request = [matchers: new MatchingRulesImpl(), generators: new Generators()] + requestData
    setupBody(requestData, request)
    if (requestData.query instanceof String) {
      request.query = PactReaderKt.queryStringToMap(requestData.query)
    } else {
      request.query = requestData.query?.collectEntries { k, v ->
        if (v instanceof Collection) {
          [k, v]
        } else {
          [k, [v]]
        }
      }
    }
    this.requestData << request
    this
  }

  /**
   * Defines the response attributes (body, headers, etc.) that are returned for the request
   * @param responseData Map of attributes
   * @return
   */
  @SuppressWarnings('DuplicateMapLiteral')
  PactBuilder willRespondWith(Map responseData) {
    def response = [matchers: new MatchingRulesImpl(), generators: new Generators()] + responseData
    setupBody(responseData, response)
    this.responseData << response
    requestState = false
    this
  }

  /**
   * Allows the body to be defined using a Groovy builder pattern
   * @param options The following options are available:
   *   - mimeType Optional mimetype for the body
   *   - prettyPrint If the body should be pretty printed
   * @param closure Body closure
   */
  PactBuilder withBody(Map options = [:], Closure closure) {
    def body = new PactBodyBuilder(mimetype: options.mimeType, prettyPrintBody: options.prettyPrint)
    closure.delegate = body
    def result = closure.call()
    if (result instanceof Matcher) {
      throw new InvalidMatcherException('Detected an invalid use of the matchers. ' +
        'If you are using matchers like "eachLike" they need to be assigned to something. For instance:\n' +
        '  `fruits eachLike(1)` or `id = integer()`'
      )
    }
    setupRequestOrResponse(body, options)
    this
  }

  /**
   * Allows the body to be defined using a Groovy builder pattern with an array as the root
   * @param options The following options are available:
   *   - mimeType Optional mimetype for the body
   *   - prettyPrint If the body should be pretty printed
   * @param array body
   */
  PactBuilder withBody(Map options = [:], List array) {
    def body = new PactBodyBuilder(mimetype: options.mimeType, prettyPrintBody: options.prettyPrint)
    body.bodyRepresentation = body.build(array)
    setupRequestOrResponse(body, options)
    this
  }

  /**
   * Allows the body to be defined using a Groovy builder pattern with an array as the root
   * @param options The following options are available:
   *   - mimeType Optional mimetype for the body
   *   - prettyPrint If the body should be pretty printed
   * @param matcher body
   */
  PactBuilder withBody(Map options = [:], LikeMatcher matcher) {
    def body = new PactBodyBuilder(mimetype: options.mimetype, prettyPrintBody: options.prettyPrint)
    body.bodyRepresentation = body.build(matcher)
    setupRequestOrResponse(body, options)
    this
  }

  private setupRequestOrResponse(PactBodyBuilder body, Map options) {
    if (requestState) {
      requestData.last().body = body.body
      requestData.last().matchers.addCategory(body.matchers)
      requestData.last().generators.addGenerators(body.generators)
      requestData.last().headers = requestData.last().headers ?: [:]
      if (!requestData.last().headers[CONTENT_TYPE]) {
        if (options.mimeType) {
          requestData.last().headers[CONTENT_TYPE] = options.mimeType
        } else {
          requestData.last().headers[CONTENT_TYPE] = JSON
        }
      }
    } else {
      responseData.last().body = body.body
      responseData.last().matchers.addCategory(body.matchers)
      responseData.last().generators.addGenerators(body.generators)
      responseData.last().headers = responseData.last().headers ?: [:]
      if (!responseData.last().headers[CONTENT_TYPE]) {
        if (options.mimeType) {
          responseData.last().headers[CONTENT_TYPE] = options.mimeType
        } else {
          responseData.last().headers[CONTENT_TYPE] = JSON
        }
      }
    }
  }

  /**
   * Executes the providers closure in the context of the interactions defined on this builder.
   * @param options Optional map of options for the run
   * @param closure Test to execute
   * @return The result of the test run
   */
  @CompileStatic
  PactVerificationResult runTest(Map options = [:], Closure closure) {
    buildInteractions()
    this.pact.interactions = interactions

    def pactVersion = options.specificationVersion ?: PactSpecVersion.V3
    MockProviderConfig config = MockProviderConfig.httpConfig(LOCALHOST, port ?: 0, pactVersion as PactSpecVersion,
      MockServerImplementation.Default)

    def runTest = closure
    if (closure.maximumNumberOfParameters < 2) {
      if (closure.maximumNumberOfParameters == 1) {
        runTest =  { MockServer server, PactTestExecutionContext context -> closure.call(server) }
      } else {
        runTest =  { MockServer server, PactTestExecutionContext context -> closure.call() }
      }
    }

    runConsumerTest(pact, config, runTest)
  }

  /**
   * Runs the test (via the runTest method), and throws an exception if it was not successful.
   * @param options Optional map of options for the run
   * @param closure
   */
  @SuppressWarnings('InvertedIfElse')
  void runTestAndVerify(Map options = [:], Closure closure) {
    PactVerificationResult result = runTest(options, closure)
    if (!(result instanceof PactVerificationResult.Ok)) {
      if (result instanceof PactVerificationResult.Error) {
        if (!(result.mockServerState instanceof PactVerificationResult.Ok)) {
          throw new AssertionError('Pact Test function failed with an exception, possibly due to ' +
            result.mockServerState, result.error)
        } else {
          throw new AssertionError('Pact Test function failed with an exception: ' + result.error.message, result.error)
        }
      }
      throw new PactFailedException(result)
    }
  }

  /**
   * Sets up a file upload request using a multipart FORM POST. This will add the correct content type header to
   * the request
   * @param partName This is the name of the part in the multipart body.
   * @param fileName This is the name of the file that was uploaded
   * @param fileContentType This is the content type of the uploaded file
   * @param data This is the actual file contents
   */
  void withFileUpload(String partName, String fileName, String fileContentType, byte[] data) {
    def multipart = MultipartEntityBuilder.create()
      .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
      .addBinaryBody(partName, data, ContentType.create(fileContentType), fileName)
      .build()
    ByteArrayOutputStream os = new ByteArrayOutputStream()
    multipart.writeTo(os)
    if (requestState) {
      requestData.last().body = os.toByteArray()
      requestData.last().headers = requestData.last().headers ?: [:]
      requestData.last().headers[CONTENT_TYPE] = multipart.contentType.value
      Category category  = requestData.last().matchers.addCategory(HEADER)
      category.addRule(CONTENT_TYPE, new RegexMatcher(Headers.MULTIPART_HEADER_REGEX, multipart.contentType.value))
    } else {
      responseData.last().body = os.toByteArray()
      responseData.last().headers = responseData.last().headers ?: [:]
      responseData.last().headers[CONTENT_TYPE] = multipart.contentType.value
      Category category  = responseData.last().matchers.addCategory(HEADER)
      category.addRule(CONTENT_TYPE, new RegexMatcher(Headers.MULTIPART_HEADER_REGEX, multipart.contentType.value))
    }
  }

  /**
   * Marks a item as to be injected from the provider state
   * @param expression Expression to lookup in the provider state context
   * @param exampleValue Example value to use in the consumer test
   * @return example value
   */
  def fromProviderState(String expression, def exampleValue) {
    new GeneratedValue(expression, exampleValue)
  }

  @Override
  @SuppressWarnings('UnnecessaryOverridingMethod')
  def call(@DelegatesTo(value = PactBuilder, strategy = Closure.DELEGATE_FIRST) Closure closure) {
    super.build(closure)
  }

  @Override
  @SuppressWarnings('UnnecessaryOverridingMethod')
  def build(@DelegatesTo(value = PactBuilder, strategy = Closure.DELEGATE_FIRST) Closure closure) {
    super.build(closure)
  }
}
