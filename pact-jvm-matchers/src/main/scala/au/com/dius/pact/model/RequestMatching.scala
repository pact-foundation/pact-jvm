package au.com.dius.pact.model

import com.typesafe.scalalogging.StrictLogging

case class RequestMatching(expectedInteractions: Seq[Interaction]) {
  import au.com.dius.pact.model.RequestMatching._
      
  def matchInteraction(actual: Request): RequestMatch = {
    def compareToActual(expected: Interaction) = compareRequest(expected, actual) 
    val matches = expectedInteractions.map(compareToActual)
    if (matches.isEmpty)
      RequestMismatch
    else
      matches.reduceLeft(_ merge _)
  }
      
  def findResponse(actual: Request): Option[Response] = 
    matchInteraction(actual).toOption.map(_.response)
}

object RequestMatching extends StrictLogging {
  import au.com.dius.pact.model.Matching._

  var diffConfig = DiffConfig(allowUnexpectedKeys = false, structural = false)

  implicit def liftPactForMatching(pact: Pact): RequestMatching = RequestMatching(pact.interactions)
                     
  def isPartialMatch(problems: Seq[RequestPartMismatch]): Boolean = !problems.exists {
    case PathMismatch(_,_,_) | MethodMismatch(_,_) => true
    case _ => false
  }
    
  def decideRequestMatch(expected: Interaction, problems: Seq[RequestPartMismatch]): RequestMatch = 
    if (problems.isEmpty) FullRequestMatch(expected)
    else if (isPartialMatch(problems)) PartialRequestMatch(expected, problems) 
    else RequestMismatch
    
  def compareRequest(expected: Interaction, actual: Request): RequestMatch = {
    val mismatches: Seq[RequestPartMismatch] = requestMismatches(expected.request, actual)
    logger.debug("Request mismatch: " + mismatches)
    decideRequestMatch(expected, mismatches)
  }
                                              
  def requestMismatches(expected: Request, actual: Request): Seq[RequestPartMismatch] = {
    (matchMethod(expected.method, actual.method) 
      ++ matchPath(expected, actual)
      ++ matchQuery(expected, actual)
      ++ matchCookie(expected.cookie, actual.cookie)
      ++ matchRequestHeaders(expected, actual)
      ++ matchBody(expected, actual, diffConfig)).toSeq
  }
}
