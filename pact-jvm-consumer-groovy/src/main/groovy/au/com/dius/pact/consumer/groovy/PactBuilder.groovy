package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.StatefulMockProvider
import au.com.dius.pact.consumer.VerificationResult
import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.MockProviderConfig
import au.com.dius.pact.model.MockProviderConfig$
import au.com.dius.pact.model.PactFragment
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.Request$
import au.com.dius.pact.model.Response$
import au.com.dius.pact.model.Interaction$
import groovy.json.JsonBuilder
import scala.None$
import scala.Some$
import scala.collection.JavaConverters$

import java.util.regex.Pattern

class PactBuilder extends Matchers {

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

  def call(Closure closure) {
    build(closure)
  }

  def build(Closure closure) {
    closure.delegate = this
    closure.call()
  }

  PactBuilder serviceConsumer(String consumer) {
    this.consumer = new Consumer(consumer)
    this
  }

  def service_consumer = this.&serviceConsumer

  PactBuilder hasPactWith(String provider) {
    this.provider = new Provider(provider)
    this
  }

  def has_pact_with = this.&hasPactWith

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

  def upon_receiving = this.&uponReceiving

  def buildInteractions() {
    int numInteractions = Math.min(requestData.size(), responseData.size())
    for (int i = 0; i < numInteractions; i++) {
      Map headers = requestData[i].headers ?: [:]
      Map responseHeaders = responseData[i].headers ?: [:]
      Map query = [:]
      def state = providerState.empty ? None$.empty() : Some$.MODULE$.apply(providerState)
      Map requestMatchers = requestData[i].matchers ?: [:]
      String path = setupPath(requestData[i].path ?: '/', requestMatchers)
      interactions << Interaction$.MODULE$.apply(
        requestDescription,
        state,
        Request$.MODULE$.apply(requestData[i].method ?: 'get', path,
          queryToString(requestData[i]?.query), headers, requestData[i].body ?: '', requestMatchers),
        Response$.MODULE$.apply(responseData[i].status ?: 200, responseHeaders,
          responseData[i].body ?: '', responseData[i].matchers)
      )
    }
    requestData = []
    responseData = []
  }

  private static String setupPath(def path, Map matchers) {
    if (path instanceof Matcher) {
      matchers['$.path'] = path.matcher
      return path.value
    } else if (path instanceof Pattern) {
      def matcher = new RegexpMatcher(values: [path])
      matchers['$.path'] = matcher.matcher
      return matcher.value
    } else {
      return path as String
    }
  }

  private static String queryToString(query) {
    if (query instanceof Map) {
      query.collect({ k, v -> (v instanceof List) ? v.collect({ "$k=$it" }) : "$k=$v" }).flatten().join('&')
    } else {
      query
    }
  }

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

  def with = this.&withAttributes

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

  def will_respond_with = this.&willRespondWith

  VerificationResult run(Closure closure) {
    buildInteractions()
    def fragment = new PactFragment(consumer, provider, JavaConverters$.MODULE$.asScalaBufferConverter(interactions).asScala())

    MockProviderConfig config
    if (port == null) {
      config = MockProviderConfig.createDefault()
    } else {
      config = MockProviderConfig$.MODULE$.apply(port, 'localhost')
    }

    fragment.runConsumer(config, closure)
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
          requestData.last().headers['Content-Type'] = mimeType
      }
    } else {
      responseData.last().body = body.body
      responseData.last().matchers.putAll(body.matchers)
      responseData.last().headers = responseData.last().headers ?: [:]
      if (mimeType) {
          responseData.last().headers['Content-Type'] = mimeType
      }
    }
    this
  }
}
