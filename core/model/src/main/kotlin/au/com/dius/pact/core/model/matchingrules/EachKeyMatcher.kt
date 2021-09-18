package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.matchingrules.expressions.MatchingRuleDefinition
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue

data class EachKeyMatcher(val definition: MatchingRuleDefinition) : MatchingRule {
  override fun toMap(spec: PactSpecVersion): Map<String, Any?> {
    val map = mutableMapOf("match" to "eachKey", "rules" to definition.rules.map { it.toMap(spec) })

    if (definition.value != null) {
      map["value"] = definition.value
    }

    if (definition.generator != null) {
      map["generator"] = definition.generator.toMap(spec)
    }

    return map
  }

  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V4) {
      listOf("eachKey matchers can only be used with Pact specification versions >= V4")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "each-key"
  override val attributes: Map<String, JsonValue>
    get() {
      val map = mutableMapOf<String, JsonValue>("rules" to JsonValue.Array(definition.rules.map {
        Json.toJson(it.toMap(PactSpecVersion.V4))
      }.toMutableList()))

      if (definition.value != null) {
        map["value"] = Json.toJson(definition.value)
      }

      if (definition.generator != null) {
        map["generator"] = Json.toJson(definition.generator.toMap(PactSpecVersion.V4))
      }

      return map
    }
}
