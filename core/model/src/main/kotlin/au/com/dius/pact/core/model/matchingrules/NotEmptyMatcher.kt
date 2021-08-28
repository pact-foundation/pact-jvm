package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.json.JsonValue

/**
 * String type matcher that checks the string length
 */
object NotEmptyMatcher : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = when (spec) {
    PactSpecVersion.V4 -> mapOf("match" to "notEmpty")
    else -> TypeMatcher.toMap(spec)
  }

  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> = listOf()

  override val name: String
    get() = "notEmpty"
  override val attributes: Map<String, JsonValue>
    get() = emptyMap()
}
