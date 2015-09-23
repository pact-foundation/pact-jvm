package au.com.dius.pact.matchers

import scala.collection.mutable

object MatchingConfig {
  var bodyMatchers = mutable.HashMap[String, BodyMatcher](
    "application/.*xml" -> new XmlBodyMatcher(),
    "application/.*json" -> new JsonBodyMatcher()
  )
}
