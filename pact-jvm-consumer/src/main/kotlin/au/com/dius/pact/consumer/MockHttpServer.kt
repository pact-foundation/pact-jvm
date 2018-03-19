package au.com.dius.pact.consumer

import au.com.dius.pact.model.FullRequestMatch
import au.com.dius.pact.model.MockHttpsProviderConfig
import au.com.dius.pact.model.MockProviderConfig
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.PartialRequestMatch
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.RequestMatching
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.RequestResponsePact
import au.com.dius.pact.model.Response
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsServer
import mu.KLogging
import org.apache.commons.lang3.StringEscapeUtils
import org.apache.http.entity.ContentType
import scala.collection.JavaConversions
import java.lang.Thread.sleep
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Returns a mock server for the pact and config
 */
fun mockServer(pact: RequestResponsePact, config: MockProviderConfig): MockServer {
  return when (config) {
    is MockHttpsProviderConfig -> MockHttpsServer(pact, config)
    else -> MockHttpServer(pact, config)
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
  fun runAndWritePact(pact: RequestResponsePact, pactVersion: PactSpecVersion, testFn: PactTestRun):
          PactVerificationResult
}

abstract class BaseMockServer(val pact: RequestResponsePact,
                              val config: MockProviderConfig,
                              private val server: HttpServer) : HttpHandler, MockServer {
  private val mismatchedRequests = ConcurrentHashMap<Request, MutableList<PactVerificationResult>>()
  private val matchedRequests = ConcurrentLinkedQueue<Request>()
  private val requestMatcher = RequestMatching.apply(JavaConversions.asScalaBuffer(pact.interactions).toSeq())

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
        pactResponseToHttpExchange(Response(500, mutableMapOf("Content-Type" to "application/json"),
          OptionalBody.body("{\"error\": ${e.message}}")), exchange)
      }
    }
  }

  private fun pactResponseToHttpExchange(response: Response, exchange: HttpExchange) {
    val headers = response.headers
    if (headers != null) {
      exchange.responseHeaders.putAll(headers.mapValues { listOf(it.value) })
    }
    val body = response.body
    if (body != null && body.isPresent()) {
      val bytes = body.unwrap().toByteArray()
      exchange.sendResponseHeaders(response.status, bytes.size.toLong())
      exchange.responseBody.write(bytes)
    } else {
      exchange.sendResponseHeaders(response.status, 0)
    }
    exchange.close()
  }

  private fun generatePactResponse(request: Request): Response {
    val matchResult = requestMatcher.matchInteraction(request)
    when (matchResult) {
      is FullRequestMatch -> {
        val interaction = matchResult.interaction() as RequestResponseInteraction
        matchedRequests.add(interaction.request)
        return interaction.response.generatedResponse()
      }
      is PartialRequestMatch -> {
        val interaction = matchResult.problems().keys().head() as RequestResponseInteraction
        mismatchedRequests.putIfAbsent(interaction.request, mutableListOf())
        mismatchedRequests[interaction.request]?.add(PactVerificationResult.PartialMismatch(
          ScalaCollectionUtils.toList(matchResult.problems()[interaction])))
      }
      else -> {
        mismatchedRequests.putIfAbsent(request, mutableListOf())
        mismatchedRequests[request]?.add(PactVerificationResult.UnexpectedRequest(request))
      }
    }
    return invalidResponse(request)
  }

  private fun invalidResponse(request: Request) =
    Response(500, mapOf("Access-Control-Allow-Origin" to "*", "Content-Type" to "application/json",
      "X-Pact-Unexpected-Request" to "1"), OptionalBody.body("{ \"error\": \"Unexpected request : " +
      StringEscapeUtils.escapeJson(request.toString()) + "\" }"))

  private fun toPactRequest(exchange: HttpExchange): Request {
    val headers = exchange.requestHeaders.mapValues { it.value.joinToString(", ") }
    val bodyContents = exchange.requestBody.bufferedReader(calculateCharset(headers)).readText()
    val body = if (bodyContents.isNullOrEmpty()) {
      OptionalBody.empty()
    } else {
      OptionalBody.body(bodyContents)
    }
    return Request(exchange.requestMethod, exchange.requestURI.path,
      PactReader.queryStringToMap(exchange.requestURI.rawQuery), headers, body)
  }

  private fun initServer() {
    server.createContext("/", this)
  }

  fun start() = server.start()

  fun stop() = server.stop(0)

  init {
    initServer()
  }

  override fun runAndWritePact(pact: RequestResponsePact, pactVersion: PactSpecVersion, testFn: PactTestRun):
          PactVerificationResult {
    start()
    waitForServer()

    try {
      testFn.run(this)
      sleep(100) // give the mock server some time to have consistent state
    } catch (e: Throwable) {
      return PactVerificationResult.Error(e, validateMockServerState())
    } finally {
      stop()
    }

    val result = validateMockServerState()
    if (result is PactVerificationResult.Ok) {
      val pactDirectory = pactDirectory()
      logger.debug { "Writing pact ${pact.consumer.name} -> ${pact.provider.name} to file " +
        "${pact.fileForPact(pactDirectory)}" }
      pact.write(pactDirectory, pactVersion)
    }

    return result
  }

  private fun validateMockServerState(): PactVerificationResult {
    if (mismatchedRequests.isNotEmpty()) {
      return PactVerificationResult.Mismatches(mismatchedRequests.values.flatten())
    }
    val expectedRequests = pact.interactions.map { it.request }.filter { !matchedRequests.contains(it) }
    if (expectedRequests.isNotEmpty()) {
      return PactVerificationResult.ExpectedButNotReceived(expectedRequests)
    }
    return PactVerificationResult.Ok
  }

  private fun waitForServer() {
    org.apache.http.client.fluent.Request.Options(getUrl())
      .addHeader("X-PACT-BOOTCHECK", "true")
      .execute()
  }

  override fun getUrl(): String {
    if (config.port == 0) {
      return "${config.scheme}://${server.address.hostName}:${server.address.port}"
    } else {
      return config.url()
    }
  }

  override fun getPort(): Int = server.address.port

  companion object : KLogging()
}

open class MockHttpServer(pact: RequestResponsePact, config: MockProviderConfig)
  : BaseMockServer(pact, config, HttpServer.create(config.address(), 0))
open class MockHttpsServer(pact: RequestResponsePact, config: MockProviderConfig)
  : BaseMockServer(pact, config, HttpsServer.create(config.address(), 0))

fun calculateCharset(headers: Map<String, String>): Charset {
  val contentType = headers.entries.find { it.key.toUpperCase() == "CONTENT-TYPE" }
  val default = Charset.forName("UTF-8")
  if (contentType != null) {
    try {
      return ContentType.parse(contentType.value)?.charset ?: default
    } catch (e: Exception) {
      BaseMockServer.Companion.logger.debug(e) { "Failed to parse the charset from the content type header" }
    }
  }
  return default
}

fun pactDirectory() = System.getProperty("pact.rootDir", "target/pacts")!!
