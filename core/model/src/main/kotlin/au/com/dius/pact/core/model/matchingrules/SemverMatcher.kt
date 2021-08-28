package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.json.JsonValue

/**
 * Matcher for semantics versions
 */
object SemverMatcher : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "semver")

  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V4) {
      listOf("Semver matchers can only be used with Pact specification versions >= V4")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "semver"
  override val attributes: Map<String, JsonValue>
    get() = emptyMap()
}
