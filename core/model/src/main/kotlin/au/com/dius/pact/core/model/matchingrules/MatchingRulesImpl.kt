package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import mu.KLogging

class MatchingRulesImpl : MatchingRules {

    val rules = mutableMapOf<String, Category>()

    override fun rulesForCategory(category: String): Category = addCategory(category)

    override fun addCategory(category: Category): Category {
        rules[category.name] = category
        return category
    }

    override fun addCategory(category: String): Category = rules.getOrPut(category, { Category(category) })

    override fun copy(): MatchingRules {
        val copy = MatchingRulesImpl()
        rules.map { it.value }.forEach { copy.addCategory(it) }
        return copy
    }

    fun fromV2Json(json: JsonValue.Object) {
      json.entries.forEach { (key, value) ->
        val path = key.split('.')
        if (key.startsWith("$.body")) {
          if (key == "$.body") {
            addV2Rule("body", "$", Json.toMap(value))
          } else {
            addV2Rule("body", "$${key.substring(6)}", Json.toMap(value))
          }
        } else if (key.startsWith("$.headers")) {
          addV2Rule("header", path[2], Json.toMap(value))
        } else {
          addV2Rule(path[1], if (path.size > 2) path[2] else null, Json.toMap(value))
        }
      }
    }

    override fun isEmpty(): Boolean = rules.all { it.value.isEmpty() }

    override fun isNotEmpty(): Boolean = !isEmpty()

    override fun hasCategory(category: String): Boolean = rules.contains(category)

    override fun getCategories(): Set<String> = rules.keys

    override fun toString(): String = "MatchingRules(rules=$rules)"
    override fun equals(other: Any?): Boolean = when (other) {
        is MatchingRulesImpl -> other.rules == rules
        else -> false
    }

    override fun hashCode(): Int = rules.hashCode()

    override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any?> = when {
        pactSpecVersion < PactSpecVersion.V3 -> toV2Map()
        else -> toV3Map()
    }

    private fun toV3Map(): Map<String, Map<String, Any?>> = rules.filter { it.value.isNotEmpty() }.mapValues { entry ->
        entry.value.toMap(PactSpecVersion.V3)
    }

    fun fromV3Json(json: JsonValue.Object) {
      json.entries.forEach { (key, value) ->
        addRules(key, Json.toMap(value))
      }
    }

    companion object : KLogging() {
      @JvmStatic
      fun fromJson(json: JsonValue?): MatchingRules {
        val matchingRules = MatchingRulesImpl()
        if (json is JsonValue.Object && json.entries.isNotEmpty()) {
          if (json.entries.keys.first().startsWith("$")) {
            matchingRules.fromV2Json(json)
          } else {
            matchingRules.fromV3Json(json)
          }
        } else logger.warn { "$json is not valid matching rules format" }
        return matchingRules
      }
    }

    private fun addRules(categoryName: String, matcherDef: Map<String, Any?>) {
      addCategory(categoryName).fromMap(matcherDef)
    }

    private fun toV2Map(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        rules.forEach { entry ->
            entry.value.toMap(PactSpecVersion.V2).forEach {
                result[it.key] = it.value
            }
        }
        return result
    }

    private fun addV2Rule(categoryName: String, item: String?, matcher: Map<String, Any?>) {
        val category = addCategory(categoryName)
        if (item != null) {
            category.addRule(item, MatchingRuleGroup.ruleFromMap(matcher))
        } else {
            category.addRule(MatchingRuleGroup.ruleFromMap(matcher))
        }
    }
}
