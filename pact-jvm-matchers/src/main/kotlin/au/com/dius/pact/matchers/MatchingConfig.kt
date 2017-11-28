package au.com.dius.pact.matchers

object MatchingConfig {
  val bodyMatchers = mapOf(
    "application/.*xml" to "au.com.dius.pact.matchers.XmlBodyMatcher",
    "application/.*json" to "au.com.dius.pact.matchers.JsonBodyMatcher",
    "application/json-rpc" to "au.com.dius.pact.matchers.JsonBodyMatcher",
    "application/jsonrequest" to "au.com.dius.pact.matchers.JsonBodyMatcher",
    "text/plain" to "au.com.dius.pact.matchers.PlainTextBodyMatcher"
  )

  @JvmStatic
  fun lookupBodyMatcher(mimeType: String): BodyMatcher? {
    val matcher = bodyMatchers.entries.find { mimeType.matches(Regex(it.key)) }?.value
    return if (matcher != null) {
      Class.forName(matcher)?.newInstance() as BodyMatcher?
    } else {
      null
    }
  }
}
