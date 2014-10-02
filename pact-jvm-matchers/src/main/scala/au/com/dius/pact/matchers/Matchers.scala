package au.com.dius.pact.matchers

import au.com.dius.pact.model.BodyMismatch
import scala.collection.mutable

object Matchers {
  def matcherDefined(path: String, matchers: Option[Map[String, Any]]): Boolean =
    matchers.isDefined && matchers.get.contains(path)

  def domatch(matcherDef: Any, path: String, expected: Any, actual: Any) : List[BodyMismatch] = {
    matcherDef match {
      case map: Map[String, Any] => matchers(map.keys.head).domatch(map, path, expected, actual)
      case _ => List(BodyMismatch(expected, actual, Some("matcher is mis-configured"), path))
    }
  }

  def matcher(matcherDef: Map[String, Any]) : Matcher = {
    matchers(matcherDef.keys.head)
  }

  val matchers = mutable.HashMap[String, Matcher]("regex" -> new RegexpMatcher)
}

trait Matcher {
  def domatch(matcherDef: Map[String, Any], path: String, expected: Any, actual: Any) : List[BodyMismatch]
}

class RegexpMatcher extends Matcher {
  def domatch(matcherDef: Map[String, Any], path: String, expected: Any, actual: Any): List[BodyMismatch] = {
    val regex = matcherDef("regex").toString
    if (actual.toString.matches(regex)) {
      List()
    } else {
      List(BodyMismatch(expected, actual, Some(s"Expected '$actual' to match '$regex'"), path))
    }
  }
}
