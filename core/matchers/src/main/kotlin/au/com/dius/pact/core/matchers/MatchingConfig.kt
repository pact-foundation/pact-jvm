package au.com.dius.pact.core.matchers

object MatchingConfig {
  val bodyMatchers = mapOf(
    "application/.*xml" to "au.com.dius.pact.core.matchers.XmlBodyMatcher",
    "text/xml" to "au.com.dius.pact.core.matchers.XmlBodyMatcher",
    "application/.*json" to "au.com.dius.pact.core.matchers.JsonBodyMatcher",
    "application/json-rpc" to "au.com.dius.pact.core.matchers.JsonBodyMatcher",
    "application/jsonrequest" to "au.com.dius.pact.core.matchers.JsonBodyMatcher",
    "text/plain" to "au.com.dius.pact.core.matchers.PlainTextBodyMatcher",
    "multipart/form-data" to "au.com.dius.pact.core.matchers.MultipartMessageBodyMatcher",
    "multipart/mixed" to "au.com.dius.pact.core.matchers.MultipartMessageBodyMatcher",
    "application/x-www-form-urlencoded" to "au.com.dius.pact.core.matchers.FormPostBodyMatcher"
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
