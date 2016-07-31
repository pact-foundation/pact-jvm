package au.com.dius.pact.consumer.groovy

@SuppressWarnings('UnusedImport')
import au.com.dius.pact.consumer.StatefulMockProvider
import au.com.dius.pact.consumer.VerificationResult
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.MockProviderConfig
import au.com.dius.pact.model.MockProviderConfig$
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.PactFragment
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.Response
import groovy.json.JsonBuilder
import scala.collection.JavaConverters$

import java.util.regex.Pattern

/**
 * Builder DSL for Pact tests
 */
@SuppressWarnings('PropertyName')
class PactBuilder extends BaseBuilder {

  private static final String PATH_MATCHER = '$.path'
  private static final String CONTENT_TYPE = 'Content-Type'
  private static final String JSON = 'application/json'
  private static final String BODY = 'body'

  Consumer consumer
  Provider provider
  Integer port = null
  String requestDescription
  List requestData = []
  List responseData = []
  List interactions = []
  StatefulMockProvider server
  List<ProviderState> providerStates = []
  boolean requestState

  /**
   * Defines the service consumer
   * @param consumer consumer name
   */
  PactBuilder serviceConsumer(String consumer) {
    this.consumer = new Consumer(consumer)
    this
  }

  /**
   * Defines the provider the consumer has a pact with
   * @param provider provider name
   */
  PactBuilder hasPactWith(String provider) {
    this.provider = new Provider(provider)
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
    this.requestDescription = requestDescription
    requestState = true
    this
  }

  def buildInteractions() {
    int numInteractions = Math.min(requestData.size(), responseData.size())
    for (int i = 0; i < numInteractions; i++) {
      Map requestMatchers = requestData[i].matchers ?: [:]
      Map responseMatchers = responseData[i].matchers ?: [:]
      Map headers = setupHeaders(requestData[i].headers ?: [:], requestMatchers)
      Map responseHeaders = setupHeaders(responseData[i].headers ?: [:], responseMatchers)
      String path = setupPath(requestData[i].path ?: '/', requestMatchers)
      interactions << new RequestResponseInteraction(
        requestDescription,
        providerStates,
        new Request(requestData[i].method ?: 'get', path, requestData[i]?.query, headers,
          requestData[i].containsKey(BODY) ? OptionalBody.body(requestData[i].body) : OptionalBody.missing(),
          requestMatchers),
        new Response(responseData[i].status ?: 200, responseHeaders,
          responseData[i].containsKey(BODY) ? OptionalBody.body(responseData[i].body) : OptionalBody.missing(),
          responseMatchers)
      )
    }
    requestData = []
    responseData = []
  }

  private static Map setupHeaders(Map headers, Map matchers) {
    headers.collectEntries { key, value ->
      if (value instanceof Matcher) {
        matchers["\$.headers.$key"] = value.matcher
        [key, value.value]
      } else if (value instanceof Pattern) {
        def matcher = new RegexpMatcher(values: [value])
        matchers["\$.headers.$key"] = matcher.matcher
        [key, matcher.value]
      } else {
        [key, value]
      }
    }
  }

  private static String setupPath(def path, Map matchers) {
    if (path instanceof Matcher) {
      matchers[PATH_MATCHER] = path.matcher
      path.value
    } else if (path instanceof Pattern) {
      def matcher = new RegexpMatcher(values: [path])
      matchers[PATH_MATCHER] = matcher.matcher
      matcher.value
    } else {
      path as String
    }
  }

  /**
   * Defines the request attributes (body, headers, etc.)
   * @param requestData Map of attributes
   */
  @SuppressWarnings('DuplicateMapLiteral')
  PactBuilder withAttributes(Map requestData) {
    def request = [matchers: [:]] + requestData
    setupBody(requestData, request)
    if (requestData.query instanceof String) {
      request.query = PactReader.queryStringToMap(requestData.query)
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

  private setupBody(Map requestData, Map request) {
    if (requestData.containsKey(BODY)) {
      def body = requestData.body
      if (body instanceof PactBodyBuilder) {
        request.body = body.body
        request.matchers.putAll(body.matchers)
      } else if (body != null && !(body instanceof String)) {
        if (requestData.prettyPrint == null && !compactMimeTypes(requestData) || requestData.prettyPrint) {
          request.body = new JsonBuilder(body).toPrettyString()
        } else {
          request.body = new JsonBuilder(body).toString()
        }
      }
    }
  }

  /**
   * Defines the response attributes (body, headers, etc.) that are returned for the request
   * @param responseData Map of attributes
   * @return
   */
  @SuppressWarnings('DuplicateMapLiteral')
  PactBuilder willRespondWith(Map responseData) {
    def response = [matchers: [:]] + responseData
    setupBody(responseData, response)
    this.responseData << response
    requestState = false
    this
  }

  private static boolean compactMimeTypes(Map reqResData) {
    reqResData.headers && reqResData.headers[CONTENT_TYPE] in COMPACT_MIME_TYPES
  }

  /**
   * Executes the providers closure in the context of the interactions defined on this builder.
   * @param options Optional map of options for the run
   * @param closure Test to execute
   * @return The result of the test run
   */
  VerificationResult run(Map options = [:], Closure closure) {
    PactFragment fragment = fragment()

    MockProviderConfig config
    def pactVersion = options.specificationVersion ?: PactSpecVersion.V2
    if (port == null) {
      config = MockProviderConfig$.MODULE$.createDefault(pactVersion)
    } else {
      config = MockProviderConfig$.MODULE$.apply(port, 'localhost', pactVersion)
    }

    fragment.runConsumer(config, closure)
  }

  PactFragment fragment() {
    buildInteractions()
    new PactFragment(consumer, provider, JavaConverters$.MODULE$.asScalaBufferConverter(interactions).asScala())
  }

  /**
   * Allows the body to be defined using a Groovy builder pattern
   * @param mimeType Optional mimetype for the body
   * @param closure Body closure
   * @deprecated Use the withBody method that takes a Map for options
   */
  @Deprecated
  PactBuilder withBody(String mimeType, Closure closure) {
    withBody(mimeType: mimeType, closure)
  }

  /**
   * Allows the body to be defined using a Groovy builder pattern with an array as the root
   * @param mimeType Optional mimetype for the body
   * @param array body
   * @deprecated Use the withBody method that takes a Map for options
   */
  @Deprecated
  PactBuilder withBody(String mimeType, List array) {
    withBody(mimeType: mimeType, array)
  }

  /**
   * Allows the body to be defined using a Groovy builder pattern with an array as the root
   * using a each like matcher for all elements of the array
   * @param mimeType Optional mimetype for the body
   * @param matcher body
   * @deprecated Use the withBody method that takes a Map for options
   */
  @Deprecated
  PactBuilder withBody(String mimeType, LikeMatcher matcher) {
    withBody(mimeType: mimeType, matcher)
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
    closure.call()
    setupBody(body, options)
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
    def body = new PactBodyBuilder(mimetype: options.mimeType, prettyPrintBody: options.prettyPrint).build(array)
    setupBody(body, options)
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
    def body = new PactBodyBuilder(mimetype: options.mimetype, prettyPrintBody: options.prettyPrint).build(matcher)
    setupBody(body, options)
    this
  }

  private setupBody(PactBodyBuilder body, Map options) {
    if (requestState) {
      requestData.last().body = body.body
      requestData.last().matchers.putAll(body.matchers)
      requestData.last().headers = requestData.last().headers ?: [:]
      if (options.mimeType) {
        requestData.last().headers[CONTENT_TYPE] = options.mimeType
      } else {
        requestData.last().headers[CONTENT_TYPE] = JSON
      }
    } else {
      responseData.last().body = body.body
      responseData.last().matchers.putAll(body.matchers)
      responseData.last().headers = responseData.last().headers ?: [:]
      if (options.mimeType) {
        responseData.last().headers[CONTENT_TYPE] = options.mimeType
      } else {
        responseData.last().headers[CONTENT_TYPE] = JSON
      }
    }
  }

  /**
   * Runs the test (via the run method), and throws an exception if it was not successful.
   * @param options Optional map of options for the run
   * @param closure
   */
  void runTestAndVerify(Map options = [:], Closure closure) {
    VerificationResult result = run(options, closure)
    if (result != PACTVERIFIED) {
      throw new PactFailedException(result)
    }
  }
}
