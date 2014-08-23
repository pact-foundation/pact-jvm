package au.com.dius.pact.model

import au.com.dius.pact.model.JsonDiff._

class JsonBodyMatcher extends BodyMatcher {
  def matchBody(expected: Option[String], actual: Option[String], diffConfig: DiffConfig) : List[BodyMismatch] = {
      implicit val autoParse = JsonDiff.autoParse _
      (expected, actual) match {
        case (None, None) => List()
        case (None, b) => if(diffConfig.structural) { List() } else { List(BodyMismatch(None, b)) }
        case (a, None) => List(BodyMismatch(a, None))
        case (Some(a), Some(b)) => if (diff(a, b, diffConfig) == noChange) List() else List(BodyMismatch(Some(a), Some(b)))
      }
  }
}
