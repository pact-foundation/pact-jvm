package au.com.dius.pact.model

import JsonDiff.DiffConfig

case class RequestMatching(expectedInteractions: Seq[Interaction]) {
  import RequestMatching._
      
      
  def findResponse(actual: Request): Option[Response] = {
    def interactionMatchesActual(ei: Interaction) = matchesRequest(ei.request, actual)    
    expectedInteractions.find(interactionMatchesActual).map(_.response)
  }
                    
}


object RequestMatching {
  import Matching._

  val diffConfig = DiffConfig(allowUnexpectedKeys = false, structural = false)

  implicit def liftPactForMatching(pact: Pact): RequestMatching = RequestMatching(pact.interactions)
                          
  def matchesRequest(expected: Request, actual: Request): Boolean = 
    compareRequests(expected, actual) == MatchFound
                                  
  def matchesInteraction(expected: Interaction, actual: Interaction): Boolean = 
    compareInteractions(expected, actual) == MatchFound
                                        
  def compareInteractions(expected: Interaction, actual: Interaction): MatchResult = 
    compareRequests(expected.request, actual.request)
                                              
  def compareRequests(expected: Request, actual: Request): MatchResult = {
    matchMethod(expected.method, actual.method) and
    matchPath(expected.path, actual.path) and
    matchCookie(expected.cookie, actual.cookie) and
    matchHeaders(expected.headersWithoutCookie, actual.headersWithoutCookie) and
    matchBodies(expected.body, actual.body, diffConfig)
  }
}

