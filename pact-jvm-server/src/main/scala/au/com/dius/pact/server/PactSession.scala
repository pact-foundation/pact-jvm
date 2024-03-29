package au.com.dius.pact.server

import au.com.dius.pact.core.matchers.{FullRequestMatch, PartialRequestMatch, RequestMatching, RequestMismatch}
import au.com.dius.pact.core.model.{Interaction, OptionalBody, Request, RequestResponseInteraction, Response, Pact => PactModel}
import org.apache.commons.lang3.StringEscapeUtils

object PactSessionResults {
  val empty = PactSessionResults(Nil, Nil, Nil, Nil)
}

case class PactSessionResults(
                               matched: List[Interaction],
                               almostMatched: List[PartialRequestMatch],
                               missing: List[Interaction],
                               unexpected: List[Request]) {

  def addMatched(inter: Interaction) = copy(matched = inter :: matched)
  def addUnexpected(request: Request) = copy(unexpected = request :: unexpected)
  def addMissing(inters: Iterable[Interaction]) = copy(missing = inters ++: missing)
  def addAlmostMatched(partial: PartialRequestMatch) = copy(almostMatched = partial :: almostMatched)

  def allMatched: Boolean = missing.isEmpty && unexpected.isEmpty
}

object PactSession {
  val empty = PactSession(None, PactSessionResults.empty)

  def forPact(pact: PactModel) = PactSession(Some(pact), PactSessionResults.empty)
}

case class PactSession(expected: Option[PactModel], results: PactSessionResults) {
  import scala.collection.JavaConverters._

  val CrossSiteHeaders = Map[String, java.util.List[String]]("Access-Control-Allow-Origin" -> List("*").asJava)

  def invalidRequest(req: Request) = {
    val headers: Map[String, java.util.List[String]] = CrossSiteHeaders ++ Map("Content-Type" -> List("application/json").asJava,
      "X-Pact-Unexpected-Request" -> List("1").asJava)
    val body = "{ \"error\": \"Unexpected request : " + StringEscapeUtils.escapeJson(req.toString) + "\" }"
    new Response(500, headers.asJava, OptionalBody.body(body.getBytes))
  }

  def receiveRequest(req: Request): (Response, PactSession) = {
    val invalidResponse = invalidRequest(req)

    val matcher = new RequestMatching(expected.get)
    matcher.matchInteraction(req) match {
      case frm: FullRequestMatch =>
        (frm.getInteraction.asInstanceOf[RequestResponseInteraction].getResponse, recordMatched(frm.getInteraction))

      case p: PartialRequestMatch =>
        (invalidResponse, recordAlmostMatched(p))

      case _: RequestMismatch =>
        (invalidResponse, recordUnexpected(req))
    }
  }

  def recordUnexpected(req: Request): PactSession =
    copy(results = results addUnexpected req)

  def recordAlmostMatched(partial: PartialRequestMatch): PactSession =
    copy(results = results addAlmostMatched partial)

  def recordMatched(interaction: Interaction): PactSession =
    copy(results = results addMatched interaction)

  def withTheRestMissing: PactSession = PactSession(None, remainingResults)

  def remainingResults: PactSessionResults = results.addMissing(expected.get.getInteractions.asScala diff results.matched)
}
