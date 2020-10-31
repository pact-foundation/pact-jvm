package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.model.MockHttpsProviderConfig
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.model.MockServerImplementation
import au.com.dius.pact.core.matchers.FullRequestMatch
import au.com.dius.pact.core.matchers.PartialRequestMatch
import au.com.dius.pact.core.matchers.RequestMatching
import au.com.dius.pact.core.model.DefaultPactWriter
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.queryStringToMap
import au.com.dius.pact.core.support.CustomServiceUnavailableRetryStrategy
import au.com.dius.pact.core.support.unwrap
import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsServer
import mu.KLogging
import org.apache.commons.lang3.StringEscapeUtils
import org.apache.http.client.methods.HttpOptions
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.BasicHttpClientConnectionManager
import java.lang.Thread.sleep
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream

/**
 * Returns a mock server for the pact and config
 */
fun mockServer(pact: RequestResponsePact, config: MockProviderConfig): BaseMockServer {
  return when (config) {
    is MockHttpsProviderConfig -> when (config.mockServerImplementation) {
      MockServerImplementation.KTorServer -> KTorMockServer(pact, config)
      else -> MockHttpsServer(pact, config)
    }
    else -> when (config.mockServerImplementation) {
      MockServerImplementation.KTorServer -> KTorMockServer(pact, config)
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
  fun <R> runAndWritePact(pact: RequestResponsePact, pactVersion: PactSpecVersion, testFn: PactTestRun<R>):
    PactVerificationResult

  /**
   * Returns the results of validating the mock server state
   */
  fun validateMockServerState(testResult: Any?): PactVerificationResult
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

abstract class BaseMockServer(val pact: RequestResponsePact, val config: MockProviderConfig) : AbstractBaseMockServer() {

  private val mismatchedRequests = ConcurrentHashMap<Request, MutableList<PactVerificationResult>>()
  private val matchedRequests = ConcurrentLinkedQueue<Request>()
  private val requestMatcher = RequestMatching(pact.interactions)

  override fun waitForServer() {
    val sf = SSLSocketFactory(TrustSelfSignedStrategy())
    val retryStrategy = CustomServiceUnavailableRetryStrategy(5, 500)
    val httpclient = HttpClientBuilder.create()
      .setConnectionManager(
        BasicHttpClientConnectionManager(
          RegistryBuilder.create<ConnectionSocketFactory>()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", sf)
            .build()
        )
      )
      .setSSLSocketFactory(sf)
      .setServiceUnavailableRetryStrategy(retryStrategy)
      .build()

    val httpOptions = HttpOptions(getUrl())
    httpOptions.addHeader("X-PACT-BOOTCHECK", "true")
    httpclient.execute(httpOptions).close()
  }

  override fun <R> runAndWritePact(pact: RequestResponsePact, pactVersion: PactSpecVersion, testFn: PactTestRun<R>):
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
    pact: RequestResponsePact,
    pactVersion: PactSpecVersion
  ): PactVerificationResult {
    val result = validateMockServerState(testResult)
    if (result is PactVerificationResult.Ok) {
      val pactDirectory = context.pactFolder
      val pactFile = pact.fileForPact(pactDirectory)
      logger.debug { "Writing pact ${pact.consumer.name} -> ${pact.provider.name} to file $pactFile" }
      val pactToWrite = if (pactVersion == PactSpecVersion.V4) {
        pact.asV4Pact().unwrap()
      } else {
        pact
      }
      DefaultPactWriter.writePact(pactFile, pactToWrite, pactVersion)
    }

    return result
  }

  override fun validateMockServerState(testResult: Any?): PactVerificationResult {
    if (mismatchedRequests.isNotEmpty()) {
      return PactVerificationResult.Mismatches(mismatchedRequests.values.flatten())
    }
    val expectedRequests = pact.interactions.asSequence()
      .map { it.request }
      .filter { !matchedRequests.contains(it) }
      .toList()
    if (expectedRequests.isNotEmpty()) {
      return PactVerificationResult.ExpectedButNotReceived(expectedRequests)
    }
    return PactVerificationResult.Ok(testResult)
  }

  protected fun generatePactResponse(request: Request): Response {
    when (val matchResult = requestMatcher.matchInteraction(request)) {
      is FullRequestMatch -> {
        val interaction = matchResult.interaction as RequestResponseInteraction
        matchedRequests.add(interaction.request)
        return interaction.response.generatedResponse(emptyMap(), GeneratorTestMode.Consumer)
      }
      is PartialRequestMatch -> {
        val interaction = matchResult.problems.keys.first() as RequestResponseInteraction
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

  private fun invalidResponse(request: Request): Response {
    val body = "{ \"error\": \"Unexpected request : ${StringEscapeUtils.escapeJson(request.toString())}\" }"
    return Response(500, mutableMapOf("Access-Control-Allow-Origin" to listOf("*"), "Content-Type" to listOf("application/json"),
      "X-Pact-Unexpected-Request" to listOf("1")), OptionalBody.body(body.toByteArray(),
      au.com.dius.pact.core.model.ContentType.JSON))
  }

  companion object : KLogging()
}

abstract class BaseJdkMockServer(
  pact: RequestResponsePact,
  config: MockProviderConfig,
  private val server: HttpServer,
  private var stopped: Boolean = false
) : HttpHandler, BaseMockServer(pact, config) {

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
        pactResponseToHttpExchange(Response(500, mutableMapOf("Content-Type" to listOf("application/json")),
          OptionalBody.body("{\"error\": ${e.message}}".toByteArray(), au.com.dius.pact.core.model.ContentType.JSON)), exchange)
      }
    }
  }

  private fun pactResponseToHttpExchange(response: Response, exchange: HttpExchange) {
    val headers = response.headers
    exchange.responseHeaders.putAll(headers)
    val body = response.body
    if (body.isPresent()) {
      val bytes = body.unwrap()
      exchange.sendResponseHeaders(response.status, bytes.size.toLong())
      exchange.responseBody.write(bytes)
    } else {
      exchange.sendResponseHeaders(response.status, 0)
    }
    exchange.close()
  }

  private fun toPactRequest(exchange: HttpExchange): Request {
    val headers = exchange.requestHeaders
    val contentType = contentType(headers)
    val bodyContents = when (bodyIsCompressed(headers.getFirst("Content-Encoding"))) {
      "gzip" -> GZIPInputStream(exchange.requestBody).readBytes()
      "deflate" -> DeflaterInputStream(exchange.requestBody).readBytes()
      else -> exchange.requestBody.readBytes()
    }
    val body = if (bodyContents.isEmpty()) {
      OptionalBody.empty()
    } else {
      OptionalBody.body(bodyContents, contentType)
    }
    return Request(exchange.requestMethod, exchange.requestURI.path,
      queryStringToMap(exchange.requestURI.rawQuery).toMutableMap(), headers.toMutableMap(), body)
  }

  private fun contentType(headers: Headers): au.com.dius.pact.core.model.ContentType {
    val contentType = headers.entries.find { it.key.toUpperCase() == "CONTENT-TYPE" }
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
    return if (config.port == 0) {
      "${config.scheme}://${server.address.hostName}:${server.address.port}"
    } else {
      config.url()
    }
  }

  override fun getPort(): Int = server.address.port

  companion object : KLogging()
}

open class MockHttpServer(pact: RequestResponsePact, config: MockProviderConfig) :
  BaseJdkMockServer(pact, config, HttpServer.create(config.address(), 0))

open class MockHttpsServer(pact: RequestResponsePact, config: MockProviderConfig) :
  BaseJdkMockServer(pact, config, HttpsServer.create(config.address(), 0))

fun calculateCharset(headers: Map<String, List<String?>>): Charset {
  val contentType = headers.entries.find { it.key.toUpperCase() == "CONTENT-TYPE" }
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
