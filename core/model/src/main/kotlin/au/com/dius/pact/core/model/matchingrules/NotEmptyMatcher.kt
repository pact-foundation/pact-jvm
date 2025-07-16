package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.json.JsonValue

/**
 * Type matcher that checks the length of the type
 */
object NotEmptyMatcher : MatchingRule {
  override fun toMap(spec: PactSpecVersion?) = when (spec) {
    PactSpecVersion.V4 -> mapOf("match" to "notEmpty")
    else -> TypeMatcher.toMap(spec)
  }

  override fun validateForVersion(pactVersion: PactSpecVersion?): List<String> = listOf()

  override fun canMatch(contentType: ContentType) = true

  override fun generateDescription(forCollection: Boolean) = "must not be empty"

  override fun validForLists() = true

  override val name: String
    get() = "not-empty"
  override val attributes: Map<String, JsonValue>
    get() = emptyMap()
}
