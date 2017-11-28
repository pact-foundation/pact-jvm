package au.com.dius.pact.matchers

import java.util.Collections

import au.com.dius.pact.model._
import au.com.dius.pact.model.matchingrules.{MatchingRules, RegexMatcher}
import com.typesafe.scalalogging.StrictLogging
import scala.collection.JavaConverters._

class PlainTextBodyMatcher extends BodyMatcher with StrictLogging {

  override def matchBody(expected: HttpPart, actual: HttpPart, allowUnexpectedKeys: Boolean): java.util.List[BodyMismatch] = {
    (expected.getBody.getState, actual.getBody.getState) match {
      case (OptionalBody.State.MISSING, _) => Collections.emptyList()
      case (OptionalBody.State.NULL, OptionalBody.State.PRESENT) => Collections.singletonList(new BodyMismatch(null, actual.getBody.getValue,
        s"Expected empty body but received '${actual.getBody.getValue}'"))
      case (OptionalBody.State.NULL, _) => Collections.emptyList()
      case (_, OptionalBody.State.MISSING) => Collections.singletonList(new BodyMismatch(expected.getBody.getValue, null,
        s"Expected body '${expected.getBody.getValue}' but was missing"))
      case (OptionalBody.State.EMPTY, OptionalBody.State.EMPTY) => Collections.emptyList()
      case (_, _) => compareText(expected.getBody.orElse(""), actual.getBody.orElse(""), expected.getMatchingRules).asJava
    }
  }

  def compareText(expected: String, actual: String, matchers: MatchingRules): List[BodyMismatch] = {
    val regex = matchers.rulesForCategory("body").getMatchingRules.get("$")

    if(regex == null || regex.getRules.isEmpty || !regex.getRules.get(0).isInstanceOf[RegexMatcher]) {
      logger.debug("No regex for " + expected + ", using equality")

      if(expected == actual) {
        return List()
      }

      return List(new BodyMismatch(expected, actual,
        s"Expected body '${expected}' to match '${actual}' using equality but did not match"))
    }

    if (actual.matches(regex.getRules.get(0).asInstanceOf[RegexMatcher].getRegex)) {
      return List()
    }

    List(new BodyMismatch(expected, actual,
      s"Expected body '${expected}' to match '${actual}' using regex '${regex.toString()}' but did not match"))
  }
}
