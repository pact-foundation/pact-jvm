package au.com.dius.pact.consumer

import au.com.dius.pact.model._
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
  import scala.collection.JavaConversions._

  val empty = PactSession(Seq(), PactSessionResults.empty)
  
  def forPact(pact: Pact) = PactSession(pact.getInteractions, PactSessionResults.empty)
}

case class PactSession(expected: Seq[Interaction], results: PactSessionResults) {
  import scala.collection.JavaConversions._
  private def matcher = RequestMatching(expected.toSeq)

  val CrossSiteHeaders = Map[String, String]("Access-Control-Allow-Origin" -> "*")

  def invalidRequest(req: Request) = {
    val headers: Map[String, String] = CrossSiteHeaders ++ Map("Content-Type" -> "application/json", "X-Pact-Unexpected-Request" -> "1")
    new Response(500, headers, "{ \"error\": \"Unexpected request : " + StringEscapeUtils.escapeJson(req.toString) + "\" }")
  }

  def receiveRequest(req: Request): (Response, PactSession) = {
    val invalidResponse = invalidRequest(req)
    
    matcher.matchInteraction(req) match {
      case FullRequestMatch(inter) => 
        (inter.getResponse, recordMatched(inter))
        
      case p @ PartialRequestMatch(problems) => 
        (invalidResponse, recordAlmostMatched(p))
        
      case RequestMismatch => 
        (invalidResponse, recordUnexpected(req))
    }
  }
  
  def recordUnexpected(req: Request): PactSession =
    copy(results = results addUnexpected req)
  
  def recordAlmostMatched(partial: PartialRequestMatch): PactSession = 
    copy(results = results addAlmostMatched partial)  
    
  def recordMatched(interaction: Interaction): PactSession = 
    copy(results = results addMatched interaction)
  
  def withTheRestMissing: PactSession = PactSession(Seq(), remainingResults)
  
  def remainingResults: PactSessionResults = results.addMissing(expected diff results.matched)
}
