package au.com.dius.pact.consumer

import au.com.dius.pact.model._


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
  val empty = PactSession(Seq(), PactSessionResults.empty)
  
  def forPact(pact: Pact) = PactSession(pact.interactions, PactSessionResults.empty)
}

case class PactSession(expected: Seq[Interaction], results: PactSessionResults) {
  private def matcher = RequestMatching(expected.toSeq)
  
  def receiveRequest(req: Request): (Response, PactSession) = {
    val invalidResponse = Response.invalidRequest(req)
    
    matcher.matchInteraction(req) match {
      case FullRequestMatch(inter) => 
        (inter.response, recordMatched(inter))
        
      case p @ PartialRequestMatch(problems) => 
        (invalidResponse, recordAlmostMatched(p))
        
      case RequestMismatch => 
        (invalidResponse, recordUnexpected(req))
    }
  }
  
  private def forgetAbout(req: Request): PactSession = 
    copy(expected = expected.filterNot(_.request == req))
  
  def recordUnexpected(req: Request): PactSession = 
    forgetAbout(req).copy(results = results addUnexpected req)
  
  def recordAlmostMatched(partial: PartialRequestMatch): PactSession = 
    copy(results = results addAlmostMatched partial)  
    
  def recordMatched(interaction: Interaction): PactSession = 
    forgetAbout(interaction.request).copy(results = results addMatched interaction)
  
  def withTheRestMissing: PactSession = PactSession(Seq(), remainingResults)
  
  def remainingResults: PactSessionResults = results addMissing expected
}


