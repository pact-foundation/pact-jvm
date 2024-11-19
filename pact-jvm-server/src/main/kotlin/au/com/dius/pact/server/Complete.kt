package au.com.dius.pact.server

import au.com.dius.pact.core.model.DefaultPactWriter
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.PactWriter
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

object Complete {

  var pactWriter: PactWriter = DefaultPactWriter
  private val CrossSiteHeaders = mapOf("Access-Control-Allow-Origin" to listOf("*"))

  fun getPort(j: Any?): String? = when(j) {
    is Map<*, *> -> {
      if (j.contains("port")) j["port"].toString()
      else null
    }
    else -> null
  }

  fun toJson(error: VerificationResult) =
    OptionalBody.body(("{\"error\": \"$error\"}").toByteArray())

  fun parseJsonString(json: String?) =
    if (json == null || json.trim().isEmpty()) null
    else Json.fromJson(JsonParser.parseString(json))

  @JvmStatic
  fun apply(request: Request, oldState: ServerState): Result {
    fun pactWritten(response: Response, port: String) = run {
      val serverState = oldState.state
      val server = serverState[port]
      val newState = ServerState(oldState.state.filter { it.value != server })
      Result(response, newState)
    }

    val port = getPort(parseJsonString(request.body.valueAsString()))
    if (port != null) {
      val mockProvider = oldState.state[port]
      if (mockProvider != null) {
        val sessionResults = mockProvider.session.remainingResults()
        val pact = mockProvider.pact
        if (pact != null) {
          mockProvider.stop()

          return when (val result = writeIfMatching(pact, sessionResults, mockProvider.config.pactVersion)) {
            is VerificationResult.PactVerified -> pactWritten(Response(200, CrossSiteHeaders.toMutableMap()),
              mockProvider.config.port.toString())
            else -> pactWritten(Response(400, mapOf("Content-Type" to listOf("application/json")).toMutableMap(),
              toJson(result)), mockProvider.config.port.toString())
          }
        }
      }
    }

    return Result(Response(400), oldState)
  }

  fun writeIfMatching(pact: Pact, results: PactSessionResults, pactVersion: PactSpecVersion): VerificationResult {
    if (results.allMatched()) {
      val pactFile = destinationFileForPact(pact)
      pactWriter.writePact(pactFile, pact, pactVersion)
    }
    return VerificationResult.apply(au.com.dius.pact.core.support.Result.Ok(results))
  }

  fun defaultFilename(pact: Pact): String = "${pact.consumer.name}-${pact.provider.name}.json"

  fun destinationFileForPact(pact: Pact): File = destinationFile(defaultFilename(pact))

  fun destinationFile(filename: String) = File("${System.getProperty("pact.rootDir", "target/pacts")}/$filename")
}
