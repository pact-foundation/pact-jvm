package au.com.dius.pact.matchers

import au.com.dius.pact.model.BodyMismatch
import scala.collection.mutable

object Matchers {
  def matcherDefined(path: String, matchers: Option[Map[String, Any]]): Boolean =
    matchers.isDefined && matchers.get.contains(path)

  def domatch(matcherDef: Any, path: String, expected: Any, actual: Any) : List[BodyMismatch] = {
    matcherDef match {
      case map: Map[String, Any] => matcher(map).domatch(path, expected, actual)
      case _ => List(BodyMismatch(expected, actual, Some("matcher is mis-configured"), path))
    }
  }

  def matcher(matcherDef: Map[String, Any]) : Matcher = {
    val matcherType = matcherDef.keys.head
    matchers(matcherType)(matcherDef(matcherType))
  }

  val matchers = mutable.HashMap[String, Class[Matcher]]("regex" -> RegexpMatcher.class)
}

trait Matcher {
  def domatch(path: String, expected: Any, actual: Any) : List[BodyMismatch]
}

class RegexpMatcher(regexp: String) extends Matcher {
  def domatch(path: String, expected: Any, actual: Any): List[BodyMismatch] = { List() }
}
