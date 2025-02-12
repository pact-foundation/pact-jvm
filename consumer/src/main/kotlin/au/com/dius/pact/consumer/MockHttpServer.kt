package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.Headers.headerToString
import au.com.dius.pact.consumer.model.MockHttpsProviderConfig
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.model.MockServerImplementation
import au.com.dius.pact.core.matchers.FullRequestMatch
import au.com.dius.pact.core.matchers.PartialRequestMatch
import au.com.dius.pact.core.matchers.RequestMatching
import au.com.dius.pact.core.matchers.generators.ArrayContainsJsonGenerator
import au.com.dius.pact.core.matchers.generators.DefaultResponseGenerator
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.DefaultPactWriter
import au.com.dius.pact.core.model.IRequest
import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.queryStringToMap
import au.com.dius.pact.core.support.Result
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsServer
import io.github.oshai.kotlinlogging.KLogging
import io.ktor.http.parseHeaderValue
import org.apache.commons.text.StringEscapeUtils
import org.apache.hc.client5.http.classic.methods.HttpOptions
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager
import org.apache.hc.client5.http.socket.ConnectionSocketFactory
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.ssl.SSLContexts
import org.apache.hc.core5.util.TimeValue
import java.lang.Thread.sleep
import java.nio.charset.Charset
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream

/**
 * Returns a mock server for the pact and config
 */
fun mockServer(pact: BasePact, config: MockProviderConfig): BaseMockServer {
  return when (config) {
    is MockHttpsProviderConfig -> when (config.mockServerImplementation) {
      MockServerImplementation.KTorServer -> KTorMockServer(pact, config)
      else -> MockHttpsServer(pact, config)
    }
    else -> when (config.mockServerImplementation) {
      MockServerImplementation.KTorServer -> KTorMockServer(pact, config)
      MockServerImplementation.Plugin -> PluginMockServer(pact, config)
      else -> MockHttpServer(pact, config)
    }
  }
}

interface MockServer {
  /**
   * Returns the URL for this mock server. The port will be the one bound by the server.
   */
  fun getUrl(): String

  /**
   * Returns the port of the mock server. This will be the port the server is bound to.
   */
  fun getPort(): Int

  /**
   * This will start the mock server and execute the test function. Returns the result of running the test.
   */
  fun <R> runAndWritePact(pact: BasePact, pactVersion: PactSpecVersion, testFn: PactTestRun<R>): PactVerificationResult

  /**
   * Returns the results of validating the mock server state
   */
  fun validateMockServerState(testResult: Any?): PactVerificationResult

  /**
   * Lets the mock server annotate the Pact when ready to be written
   */
  fun updatePact(pact: Pact): Pact
}

abstract class AbstractBaseMockServer : MockServer {
  abstract fun start()
  abstract fun stop()
  abstract fun waitForServer()

  protected fun bodyIsCompressed(encoding: String?): String? {
    return if (COMPRESSED_ENCODINGS.contains(encoding)) encoding else null
  }

  companion object : KLogging() {
    val COMPRESSED_ENCODINGS = setOf("gzip", "deflate")
  }
}

abstract class BaseMockServer(val pact: BasePact, val config: MockProviderConfig) : AbstractBaseMockServer() {

  val mismatchedRequests = ConcurrentHashMap<IRequest, MutableList<PactVerificationResult>>()
  val matchedRequests = ConcurrentLinkedQueue<Pair<IRequest, IRequest>>()
  private val requestMatcher = RequestMatching(pact)

  override fun waitForServer() {
    val sslcontext = SSLContexts.custom().loadTrustMaterial(TrustSelfSignedStrategy()).build()
    val sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
      .setSslContext(sslcontext).build()
    val httpclient = HttpClientBuilder.create()
      .setConnectionManager(
        BasicHttpClientConnectionManager(
          RegistryBuilder.create<ConnectionSocketFactory>()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", sslSocketFactory)
            .build()
        )
      )
      .setRetryStrategy(DefaultHttpRequestRetryStrategy(5, TimeValue.ofMilliseconds(500)))
      .build()

    val httpOptions = HttpOptions(getUrl())
    httpOptions.addHeader("X-PACT-BOOTCHECK", "true")
    httpclient.execute(httpOptions).close()
  }

  @Suppress("TooGenericExceptionCaught")
  override fun <R> runAndWritePact(pact: BasePact, pactVersion: PactSpecVersion, testFn: PactTestRun<R>):
    PactVerificationResult {
    start()
    waitForServer()

    val context = PactTestExecutionContext()
    val testResult: R
    try {
      testResult = testFn.run(this, context)
      sleep(100) // give the mock server some time to have consistent state
    } catch (e: Throwable) {
      logger.debug(e) { "Caught exception in mock server" }
      return PactVerificationResult.Error(e, validateMockServerState(null))
    } finally {
      stop()
    }

    return verifyResultAndWritePact(testResult, context, pact, pactVersion)
  }

  fun <R> verifyResultAndWritePact(
    testResult: R,
    context: PactTestExecutionContext,
    pact: BasePact,
    pactVersion: PactSpecVersion
  ): PactVerificationResult {
    val result = validateMockServerState(testResult)
    if (result is PactVerificationResult.Ok) {
      val pactDirectory = context.pactFolder
      val pactFile = pact.fileForPact(pactDirectory)
      logger.debug { "Writing pact ${pact.consumer.name} -> ${pact.provider.name} to file $pactFile" }

      val pactToWrite = if (pactVersion == PactSpecVersion.V4) {
        updatePact(pact.asV4Pact().unwrap())
      } else {
        updatePact(pact)
      }

      DefaultPactWriter.writePact(pactFile, pactToWrite, pactVersion)
    }

    return result
  }

  override fun validateMockServerState(testResult: Any?): PactVerificationResult {
    if (mismatchedRequests.isNotEmpty()) {
      return PactVerificationResult.Mismatches(mismatchedRequests.values.flatten())
    }
    val receivedRequests = matchedRequests.map { it.first }
    val expectedRequests = pact.interactions.asSequence()
      .filter { it.isSynchronousRequestResponse() }
      .map { it.asSynchronousRequestResponse()!!.request }
      .filter { !receivedRequests.contains(it) }
      .toList()
    if (expectedRequests.isNotEmpty()) {
      return PactVerificationResult.ExpectedButNotReceived(expectedRequests)
    }
    return PactVerificationResult.Ok(testResult)
  }

  protected fun generatePactResponse(request: IRequest): IResponse {
    when (val matchResult = requestMatcher.matchInteraction(request)) {
      is FullRequestMatch -> {
        val interaction = matchResult.interaction
        matchedRequests.add(interaction.request to request)
        return DefaultResponseGenerator.generateResponse(interaction.response,
          mutableMapOf(
            "mockServer" to mapOf("href" to getUrl(), "port" to getPort()),
            "ArrayContainsJsonGenerator" to ArrayContainsJsonGenerator
        ), GeneratorTestMode.Consumer, emptyList(), emptyMap()) // TODO: need to pass any plugin config here
      }
      is PartialRequestMatch -> {
        logger.error { "PartialRequestMatch: ${matchResult.description()}" }
        val interaction = matchResult.problems.keys.first().asSynchronousRequestResponse()!!
        mismatchedRequests.putIfAbsent(interaction.request, mutableListOf())
        mismatchedRequests[interaction.request]?.add(PactVerificationResult.PartialMismatch(
          matchResult.problems[interaction]!!.mismatches))
      }
      else -> {
        mismatchedRequests.putIfAbsent(request, mutableListOf())
        mismatchedRequests[request]?.add(PactVerificationResult.UnexpectedRequest(request))
      }
    }
    return invalidResponse(request)
  }

  private fun invalidResponse(request: IRequest): IResponse {
    val body = "{ \"error\": \"Unexpected request : ${StringEscapeUtils.escapeJson(request.toString())}\" }"
    return Response(500,
      mutableMapOf(
        "Access-Control-Allow-Origin" to listOf("*"),
        "Content-Type" to listOf("application/json"),
        "X-Pact-Unexpected-Request" to listOf("1")
      ),
      OptionalBody.body(body.toByteArray(),
      au.com.dius.pact.core.model.ContentType.JSON)
    )
  }

  companion object : KLogging()
}

abstract class BaseJdkMockServer(
  pact: BasePact,
  config: MockProviderConfig,
  private val server: HttpServer,
  private var stopped: Boolean = false
) : HttpHandler, BaseMockServer(pact, config) {

  @Suppress("TooGenericExceptionCaught")
  override fun handle(exchange: HttpExchange) {
    if (exchange.requestMethod == "OPTIONS" && exchange.requestHeaders.containsKey("X-PACT-BOOTCHECK")) {
      exchange.responseHeaders.add("X-PACT-BOOTCHECK", "true")
      exchange.sendResponseHeaders(200, 0)
      exchange.close()
    } else {
      try {
        val request = toPactRequest(exchange)
        logger.debug { "Received request: $request" }
        val response = generatePactResponse(request)
        logger.debug { "Generating response: $response" }
        pactResponseToHttpExchange(response, exchange)
      } catch (e: Exception) {
        logger.error(e) { "Failed to generate response" }
        pactResponseToHttpExchange(Response(500,
          mutableMapOf(
            "Content-Type" to listOf("application/json")
          ),
          OptionalBody.body("{\"error\": ${e.message}}".toByteArray(),
            au.com.dius.pact.core.model.ContentType.JSON)
        ), exchange)
      }
    }
  }

  private fun pactResponseToHttpExchange(response: IResponse, exchange: HttpExchange) {
    val headers = response.headers
    if (headers.isNotEmpty()) {
      exchange.responseHeaders.putAll(headers)
    }
    if (config.addCloseHeader) {
      exchange.responseHeaders.add("Connection", "close")
    }
    val body = response.body
    if (body.isPresent()) {
      val bytes = body.unwrap()
      exchange.sendResponseHeaders(response.status, bytes.size.toLong())
      exchange.responseBody.write(bytes)
    } else {
      exchange.sendResponseHeaders(response.status, -1)
    }
    exchange.close()
  }

  fun toPactRequest(exchange: HttpExchange): Request {
    val headers = exchange.requestHeaders.mapValues { entry ->
      if (entry.value.size == 1 && Headers.isKnowMultiValueHeader(entry.key)) {
        parseHeaderValue(entry.value[0]).map { headerToString(it) }
      } else {
        entry.value
      }
    }
    val contentType = contentType(exchange.requestHeaders)
    val bodyContents = when (bodyIsCompressed(exchange.requestHeaders.getFirst("Content-Encoding"))) {
      "gzip" -> GZIPInputStream(exchange.requestBody).readBytes()
      "deflate" -> DeflaterInputStream(exchange.requestBody).readBytes()
      else -> exchange.requestBody.readBytes()
    }
    val body = if (bodyContents.isEmpty()) {
      OptionalBody.empty()
    } else {
      OptionalBody.body(bodyContents, contentType)
    }
    return Request(exchange.requestMethod, exchange.requestURI.rawPath,
      queryStringToMap(exchange.requestURI.rawQuery).toMutableMap(), headers.toMutableMap(), body)
  }

  private fun contentType(headers: com.sun.net.httpserver.Headers): au.com.dius.pact.core.model.ContentType {
    val contentType = headers.entries.find { it.key.uppercase(Locale.getDefault()) == "CONTENT-TYPE" }
    return if (contentType != null && contentType.value.isNotEmpty()) {
      au.com.dius.pact.core.model.ContentType(contentType.value.first())
    } else {
      au.com.dius.pact.core.model.ContentType.JSON
    }
  }

  private fun initServer() {
    server.createContext("/", this)
  }

  override fun start() {
    logger.debug { "Starting mock server" }
    server.start()
    logger.debug { "Mock server started: ${server.address}" }
  }

  override fun stop() {
    if (!stopped) {
      stopped = true
      server.stop(0)
      logger.debug { "Mock server shutdown" }
    }
  }

  init {
    initServer()
  }

  override fun getUrl(): String {
    // Stupid GitHub Windows agents
    val host = if (server.address.hostName.lowercase() == "miningmadness.com") {
      config.hostname
    } else {
      server.address.hostName
    }
    return "${config.scheme}://$host:${server.address.port}"
  }

  override fun getPort(): Int = server.address.port

  companion object : KLogging()
}

open class MockHttpServer(pact: BasePact, config: MockProviderConfig) :
  BaseJdkMockServer(pact, config, HttpServer.create(config.address(), 0)) {
  override fun updatePact(pact: Pact): Pact {
    return if (pact.isV4Pact()) {
      when (val p = pact.asV4Pact()) {
        is Result.Ok -> {
          for (interaction in p.value.interactions) {
            interaction.asV4Interaction().transport = "http"
          }
          p.value
        }
        is Result.Err -> pact
      }
    } else {
      pact
    }
  }
}

open class MockHttpsServer(pact: BasePact, config: MockProviderConfig) :
  BaseJdkMockServer(pact, config, HttpsServer.create(config.address(), 0)) {
  override fun updatePact(pact: Pact): Pact {
    return if (pact.isV4Pact()) {
      when (val p = pact.asV4Pact()) {
        is Result.Ok -> {
          for (interaction in p.value.interactions) {
            interaction.asV4Interaction().transport = "https"
          }
          p.value
        }
        is Result.Err -> pact
      }
    } else {
      pact
    }
  }
}

@Suppress("TooGenericExceptionCaught")
fun calculateCharset(headers: Map<String, List<String?>>): Charset {
  val contentType = headers.entries.find { it.key.lowercase() == "content-type" }
  val default = Charset.forName("UTF-8")
  if (contentType != null && contentType.value.isNotEmpty() && !contentType.value.first().isNullOrEmpty()) {
    try {
      return ContentType.parse(contentType.value.first())?.charset ?: default
    } catch (e: Exception) {
      BaseJdkMockServer.logger.debug(e) { "Failed to parse the charset from the content type header" }
    }
  }
  return default
}

fun interactionCatalogueEntries() = au.com.dius.pact.core.matchers.interactionCatalogueEntries()
