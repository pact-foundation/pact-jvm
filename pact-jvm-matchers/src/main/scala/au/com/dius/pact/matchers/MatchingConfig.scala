package au.com.dius.pact.matchers

import scala.collection.mutable

object MatchingConfig {
  var bodyMatchers = mutable.HashMap[String, BodyMatcher](
    "application/.*xml" -> new XmlBodyMatcher(),
    "application/.*json" -> new JsonBodyMatcher(),
    "application/json-rpc" -> new JsonBodyMatcher(),
    "application/jsonrequest" -> new JsonBodyMatcher()
  )

  def lookupBodyMatcher(mimeType: String): Option[(String, BodyMatcher)] = {
    bodyMatchers.find(entry => mimeType.matches(entry._1))
  }
}
