package au.com.dius.pact.server

import au.com.dius.pact.core.matchers.FullRequestMatch
import au.com.dius.pact.core.matchers.PartialRequestMatch
import au.com.dius.pact.core.matchers.RequestMatching
import au.com.dius.pact.core.matchers.RequestMismatch
import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.Response
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.text.StringEscapeUtils

private val logger = KotlinLogging.logger {}

data class PactSession(
  val expected: Pact?,
  val results: PactSessionResults
) {
  fun receiveRequest(req: Request): Pair<IResponse, PactSession> {
    val invalidResponse = invalidRequest(req)

    return if (expected != null) {
      val matcher = RequestMatching(expected)
      when (val result = matcher.matchInteraction(req)) {
        is FullRequestMatch ->
          (result.interaction.asSynchronousRequestResponse()!!.response to recordMatched(result.interaction))

        is PartialRequestMatch ->
          (invalidResponse to recordAlmostMatched(result))

        is RequestMismatch ->
          (invalidResponse to recordUnexpected(req))
      }
    } else {
      logger.warn { "Expected Pact is not set!" }
      invalidResponse to this
    }
  }

  fun recordUnexpected(req: Request) = this.copy(results = results.addUnexpected(req))

  fun recordAlmostMatched(partial: PartialRequestMatch) = this.copy(results = results.addAlmostMatched(partial))

  fun recordMatched(interaction: Interaction) = this.copy(results = results.addMatched(interaction))

  fun remainingResults() = if (expected != null)
    results.addMissing((expected.interactions - results.matched.toSet()).asIterable())
    else results

  companion object {
    val CrossSiteHeaders = mapOf("Access-Control-Allow-Origin" to listOf("*"))
    @JvmStatic
    val empty = PactSession(null, PactSessionResults.empty)

    @JvmStatic
    fun forPact(pact: Pact) = PactSession(pact, PactSessionResults.empty)

    @JvmStatic
    fun invalidRequest(req: Request): IResponse {
      val headers = CrossSiteHeaders + mapOf(
        "Content-Type" to listOf("application/json"),
        "X-Pact-Unexpected-Request" to listOf("1")
      )
      val body = "{ \"error\": \"Unexpected request : " + StringEscapeUtils.escapeJson(req.toString()) + "\" }"
      return Response(500, headers.toMutableMap(), OptionalBody.body(body.toByteArray()))
    }
  }
}
