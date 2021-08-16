package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.Headers
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.model.MockServerImplementation
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.SynchronousRequestResponse
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.support.json.JsonValue
import groovy.transform.CompileStatic
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.core5.http.ContentType

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest

/**
 * Builder DSL for Pact tests
 */
@SuppressWarnings('PropertyName')
class PactBuilder extends GroovyBuilder {

  public static final String TEXT = 'text'

  BasePact pact = new RequestResponsePact(new Provider(), new Consumer())
  Integer port = 0
  SynchronousRequestResponse currentInteraction
  List interactions = []
  List<ProviderState> providerStates = []
  boolean requestState
  PactSpecVersion specVersion = PactSpecVersion.V3

  PactBuilder(PactSpecVersion version = PactSpecVersion.V3) {
    this.specVersion = version

    if (specVersion == PactSpecVersion.V4) {
      pact = new V4Pact(new Consumer(), new Provider())
    }
  }

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
  @CompileStatic
  PactBuilder given(String providerState) {
    if (requestState && currentInteraction != null) {
      currentInteraction.providerStates << new ProviderState(providerState)
    } else {
      this.providerStates << new ProviderState(providerState)
    }
    this
  }

  /**
   * Defines the provider state the provider needs to be in for the interaction
   * @param providerState provider state description
   * @param params Data parameters for the provider state
   */
  @CompileStatic
  PactBuilder given(String providerState, Map params) {
    if (requestState && currentInteraction != null) {
      currentInteraction.providerStates << new ProviderState(providerState, params)
    } else {
      this.providerStates << new ProviderState(providerState, params)
    }
    this
  }

  /**
   * Defines the start of an interaction
   * @param requestDescription Description of the interaction. Must be unique.
   */
  PactBuilder uponReceiving(String requestDescription) {
    updateInteractions()
    if (this.specVersion == PactSpecVersion.V4) {
      this.currentInteraction = new V4Interaction.SynchronousHttp('', requestDescription, providerStates)
    } else {
      this.currentInteraction = new RequestResponseInteraction(requestDescription, providerStates)
    }
    requestState = true
    providerStates = []
    this
  }

  def updateInteractions() {
    if (currentInteraction) {
      interactions << currentInteraction
    }
  }

  /**
   * Defines the request attributes (body, headers, etc.)
   * @param requestData Map of attributes
   */
  PactBuilder withAttributes(Map requestData) {
    MatchingRules requestMatchers = currentInteraction.request.matchingRules
    Generators requestGenerators = currentInteraction.request.generators
    Map headers = setupHeaders(requestData.headers ?: [:], requestMatchers, requestGenerators)
    Map query = setupQueryParameters(requestData.query ?: [:], requestMatchers, requestGenerators)
    String path = setupPath(requestData.path ?: '/', requestMatchers, requestGenerators)
    this.currentInteraction.request.method = requestData.method ?: 'GET'
    this.currentInteraction.request.headers.putAll(headers)
    this.currentInteraction.request.query.putAll(query)
    this.currentInteraction.request.path = path
    def requestBody = setupBody(requestData, currentInteraction.request)
    this.currentInteraction.request.body = requestBody
    this
  }

  /**
   * Defines the response attributes (body, headers, etc.) that are returned for the request
   * @param responseData Map of attributes
   * @return
   */
  @SuppressWarnings('DuplicateMapLiteral')
  PactBuilder willRespondWith(Map responseData) {
    MatchingRules responseMatchers = currentInteraction.response.matchingRules
    Generators responseGenerators = currentInteraction.response.generators
    Map responseHeaders = setupHeaders(responseData.headers ?: [:], responseMatchers, responseGenerators)
    if (responseData.status instanceof StatusCodeMatcher) {
      this.currentInteraction.response.status = responseData.status.defaultStatus()
      responseMatchers.addCategory('status').addRule(responseData.status.matcher)
    } else {
      this.currentInteraction.response.status = responseData.status ?: 200
    }
    this.currentInteraction.response.headers.putAll(responseHeaders)
    def responseBody = setupBody(responseData, currentInteraction.response)
    this.currentInteraction.response.body = responseBody
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
      if (!currentInteraction.request.contentTypeHeader()) {
        if (options.mimeType) {
          currentInteraction.request.headers[CONTENT_TYPE] = [ options.mimeType ]
        } else {
          currentInteraction.request.headers[CONTENT_TYPE] = [ JSON ]
        }
      }
      currentInteraction.request.body = body.body instanceof OptionalBody ? body.body :
        OptionalBody.body(body.body.bytes)
      currentInteraction.request.matchingRules.addCategory(body.matchers)
      currentInteraction.request.generators.addGenerators(body.generators)
    } else {
      if (!currentInteraction.response.contentTypeHeader()) {
        if (options.mimeType) {
          currentInteraction.response.headers[CONTENT_TYPE] = [ options.mimeType ]
        } else {
          currentInteraction.response.headers[CONTENT_TYPE] = [ JSON ]
        }
      }
      currentInteraction.response.body = body.body instanceof OptionalBody ? body.body :
        OptionalBody.body(body.body.bytes)
      currentInteraction.response.matchingRules.addCategory(body.matchers)
      currentInteraction.response.generators.addGenerators(body.generators)
    }
  }

  /**
   * Executes the providers closure in the context of the interactions defined on this builder.
   * @param options Optional map of options for the run
   * @param closure Test to execute
   * @return The result of the test run
   */
  @CompileStatic
  @SuppressWarnings('UnusedMethodParameter')
  PactVerificationResult runTest(Map options = [:], Closure closure) {
    updateInteractions()
    this.pact.interactions.addAll(interactions)

    MockProviderConfig config = MockProviderConfig.httpConfig(LOCALHOST, port ?: 0, specVersion,
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
    ContentType contentType = ContentType.DEFAULT_TEXT
    if (!fileContentType.isEmpty()) {
      contentType = ContentType.parseLenient(fileContentType)
    }

    def multipart = MultipartEntityBuilder.create()
      .setMode(HttpMultipartMode.EXTENDED)
      .addBinaryBody(partName, data, contentType, fileName)
      .build()
    ByteArrayOutputStream os = new ByteArrayOutputStream()
    multipart.writeTo(os)
    if (requestState) {
      currentInteraction.request.body = OptionalBody.body(os.toByteArray())
      currentInteraction.request.headers[CONTENT_TYPE] = [ multipart.contentType ]
      MatchingRuleCategory category  = currentInteraction.request.matchingRules.addCategory(HEADER)
      category.addRule(CONTENT_TYPE, new RegexMatcher(Headers.MULTIPART_HEADER_REGEX, multipart.contentType))
    } else {
      currentInteraction.response.body = OptionalBody.body(os.toByteArray())
      currentInteraction.response.headers[CONTENT_TYPE] = [ multipart.contentType ]
      MatchingRuleCategory category  = currentInteraction.response.matchingRules.addCategory(HEADER)
      category.addRule(CONTENT_TYPE, new RegexMatcher(Headers.MULTIPART_HEADER_REGEX, multipart.contentType))
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

  /**
   * Adds a comment to either the request of response
   */
  PactBuilder comment(String comment) {
    if (!currentInteraction.comments.containsKey(TEXT)) {
      currentInteraction.comments [TEXT] = new JsonValue.Array()
    }
    currentInteraction.comments[TEXT].append(new JsonValue.StringValue(comment.chars))
    this
  }

  /**
   * Sets the name of the test
   */
  PactBuilder testname(String testname) {
    currentInteraction.comments['testname'] = new JsonValue.StringValue(testname.chars)
    this
  }
}
