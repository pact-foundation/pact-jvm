package au.com.dius.pact.consumer.groovy

@SuppressWarnings(['UnusedImport', 'DuplicateImport'])
import au.com.dius.pact.consumer.StatefulMockProvider
import au.com.dius.pact.consumer.VerificationResult
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.Interaction$
import au.com.dius.pact.model.MockProviderConfig
import au.com.dius.pact.model.MockProviderConfig$
import au.com.dius.pact.model.PactFragment
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.Request$
import au.com.dius.pact.model.Response$
import groovy.json.JsonBuilder
import scala.None$
import scala.Some$
import scala.collection.JavaConverters$

import java.util.regex.Pattern

/**
 * Builder DSL for Pact tests
 */
@SuppressWarnings('PropertyName')
class PactBuilder extends BaseBuilder {

  private static final String PATH_MATCHER = '$.path'
  private static final String CONTENT_TYPE = 'Content-Type'
  Consumer consumer
  Provider provider
  Integer port = null
  String requestDescription
  List requestData = []
  List responseData = []
  List interactions = []
  StatefulMockProvider server
  String providerState = ''
  boolean requestState

  PactBuilder serviceConsumer(String consumer) {
    this.consumer = new Consumer(consumer)
    this
  }

  /**
   * @deprecated Use serviceConsumer instead
   */
  @Deprecated
  def service_consumer = this.&serviceConsumer

  PactBuilder hasPactWith(String provider) {
    this.provider = new Provider(provider)
    this
  }

  /**
   * @deprecated Use hasPactWith instead
   */
  @Deprecated
  def has_pact_with = this.&hasPactWith

  @SuppressWarnings('ConfusingMethodName')
  PactBuilder port(int port) {
    this.port = port
    this
  }

  PactBuilder given(String providerState) {
    this.providerState = providerState
    this
  }

  PactBuilder uponReceiving(String requestDescription) {
    buildInteractions()
    this.requestDescription = requestDescription
    requestState = true
    this
  }

  /**
   * @deprecated Use uponReceiving instead
   */
  @Deprecated
  def upon_receiving = this.&uponReceiving

  def buildInteractions() {
    int numInteractions = Math.min(requestData.size(), responseData.size())
    for (int i = 0; i < numInteractions; i++) {
      Map requestMatchers = requestData[i].matchers ?: [:]
      Map responseMatchers = responseData[i].matchers ?: [:]
      Map headers = setupHeaders(requestData[i].headers ?: [:], requestMatchers)
      Map responseHeaders = setupHeaders(responseData[i].headers ?: [:], responseMatchers)
      def state = providerState.empty ? None$.empty() : Some$.MODULE$.apply(providerState)
      String path = setupPath(requestData[i].path ?: '/', requestMatchers)
      interactions << Interaction$.MODULE$.apply(
        requestDescription,
        state,
        Request$.MODULE$.apply(requestData[i].method ?: 'get', path,
          queryToString(requestData[i]?.query), headers, requestData[i].body ?: '', requestMatchers),
        Response$.MODULE$.apply(responseData[i].status ?: 200, responseHeaders, responseData[i].body ?: '',
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

  private static String queryToString(query) {
    if (query instanceof Map) {
      query.collectMany { k, v -> (v instanceof List) ? v.collect { "$k=$it" } : ["$k=$v"] }.join('&')
    } else {
      query
    }
  }

  @SuppressWarnings('DuplicateMapLiteral')
  PactBuilder withAttributes(Map requestData) {
    def request = [matchers: [:]] + requestData
    def body = requestData.body
    if (body instanceof PactBodyBuilder) {
      request.body = body.body
      request.matchers.putAll(body.matchers)
    } else if (body != null && !(body instanceof String)) {
      request.body = new JsonBuilder(body).toPrettyString()
    }
    this.requestData << request
    this
  }

  /**
   * @deprecated Use withAttributes instead
   */
  @Deprecated
  def with = this.&withAttributes

  @SuppressWarnings('DuplicateMapLiteral')
  PactBuilder willRespondWith(Map responseData) {
    def response = [matchers: [:]] + responseData
    def body = responseData.body
    if (body instanceof PactBodyBuilder) {
      response.body = body.body
      response.matchers.putAll(body.matchers)
    } else if (body != null && !(body instanceof String)) {
      response.body = new JsonBuilder(body).toPrettyString()
    }
    this.responseData << response
    requestState = false
    this
  }

  /**
   * @deprecated Use willRespondWith instead
   */
  @Deprecated
  def will_respond_with = this.&willRespondWith

  VerificationResult run(Closure closure) {
    PactFragment fragment = fragment()

    MockProviderConfig config
    if (port == null) {
      config = MockProviderConfig.createDefault()
    } else {
      config = MockProviderConfig$.MODULE$.apply(port, 'localhost')
    }

    fragment.runConsumer(config, closure)
  }

  PactFragment fragment() {
    buildInteractions()
    new PactFragment(consumer, provider, JavaConverters$.MODULE$.asScalaBufferConverter(interactions).asScala())
  }

  PactBuilder withBody(String mimeType = null, Closure closure) {
    def body = new PactBodyBuilder()
    closure.delegate = body
    closure.call()
    if (requestState) {
      requestData.last().body = body.body
      requestData.last().matchers.putAll(body.matchers)
      requestData.last().headers = requestData.last().headers ?: [:]
      if (mimeType) {
          requestData.last().headers[CONTENT_TYPE] = mimeType
      }
    } else {
      responseData.last().body = body.body
      responseData.last().matchers.putAll(body.matchers)
      responseData.last().headers = responseData.last().headers ?: [:]
      if (mimeType) {
          responseData.last().headers[CONTENT_TYPE] = mimeType
      }
    }
    this
  }

  void runTestAndVerify(Closure closure) {
    VerificationResult result = run(closure)
    if (result != PACTVERIFIED) {
      throw new PactFailedException(result)
    }
  }
}
