package au.com.dius.pact.model

import au.com.dius.pact.matchers.Mismatch
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConversions

case class RequestMatching(expectedInteractions: Seq[RequestResponseInteraction]) {
  import au.com.dius.pact.model.RequestMatching._
      
  def matchInteraction(actual: Request): RequestMatch = {
    def compareToActual(expected: RequestResponseInteraction) = compareRequest(expected, actual)
    val matches = expectedInteractions.map(compareToActual)
    if (matches.isEmpty)
      RequestMismatch
    else
      matches.reduceLeft(_ merge _)
  }
      
  def findResponse(actual: Request): Option[Response] = 
    matchInteraction(actual).toOption.map(_.asInstanceOf[RequestResponseInteraction].getResponse)
}

object RequestMatching extends StrictLogging {
  import au.com.dius.pact.model.Matching._
  import scala.collection.JavaConverters._

  var allowUnexpectedKeys = false

  implicit def liftPactForMatching(pact: RequestResponsePact): RequestMatching =
    RequestMatching(JavaConversions.collectionAsScalaIterable(pact.getInteractions).toSeq)
                     
  def isPartialMatch(problems: Seq[Mismatch]): Boolean = !problems.exists {
    case PathMismatch(_,_,_) | MethodMismatch(_,_) => true
    case _ => false
  }
    
  def decideRequestMatch(expected: RequestResponseInteraction, problems: Seq[Mismatch]): RequestMatch =
    if (problems.isEmpty) FullRequestMatch(expected)
    else if (isPartialMatch(problems)) PartialRequestMatch(expected, problems) 
    else RequestMismatch
    
  def compareRequest(expected: RequestResponseInteraction, actual: Request): RequestMatch = {
    val mismatches: Seq[Mismatch] = requestMismatches(expected.getRequest, actual)
    logger.debug("Request mismatch: " + mismatches)
    decideRequestMatch(expected, mismatches)
  }
                                              
  def requestMismatches(expected: Request, actual: Request): Seq[Mismatch] = {
    logger.debug("comparing to expected request: \n" + expected)
    (matchMethod(expected.getMethod, actual.getMethod)
      ++ matchPath(expected, actual)
      ++ matchQuery(expected, actual)
      ++ matchCookie(au.com.dius.pact.matchers.util.CollectionUtils.toOptionalList(expected.cookie),
      au.com.dius.pact.matchers.util.CollectionUtils.toOptionalList(actual.cookie))
      ++ au.com.dius.pact.matchers.Matching.matchRequestHeaders(expected, actual).asScala
      ++ matchBody(expected, actual, allowUnexpectedKeys)).toSeq
  }
}
