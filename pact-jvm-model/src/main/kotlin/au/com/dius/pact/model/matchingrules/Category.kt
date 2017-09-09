package au.com.dius.pact.model.matchingrules

import au.com.dius.pact.model.PactSpecVersion
import mu.KLogging
import java.util.function.Predicate
import java.util.function.ToIntFunction

/**
 * Matching rules category
 */
data class Category @JvmOverloads constructor(val name: String,
                                              var matchingRules: MutableMap<String, MatchingRuleGroup> =
                                              mutableMapOf()) {

  companion object : KLogging()

  fun addRule(item: String, matchingRule: MatchingRule) {
    if (!matchingRules.containsKey(item)) {
      matchingRules[item] = MatchingRuleGroup(mutableListOf(matchingRule))
    } else {
      matchingRules[item]!!.rules.add(matchingRule)
    }
  }

  fun addRule(matchingRule: MatchingRule) = addRule("", matchingRule)

  fun setRule(item: String, matchingRule: MatchingRule) {
    matchingRules[item] = MatchingRuleGroup(mutableListOf(matchingRule))
  }

  fun setRule(matchingRule: MatchingRule) = setRule("", matchingRule)

  fun setRules(item: String, rules: List<MatchingRule>) {
    setRules(item, MatchingRuleGroup(rules.toMutableList()))
  }

  fun setRules(matchingRules: List<MatchingRule>) = setRules("", matchingRules)

  fun setRules(item: String, rules: MatchingRuleGroup) {
    matchingRules[item] = rules
  }

  /**
   * If the rules are empty
   */
  fun isEmpty() = matchingRules.isEmpty() || matchingRules.all { it.value.rules.isEmpty() }

  /**
   * If the rules are not empty
   */
  fun isNotEmpty() = matchingRules.any { it.value.rules.isNotEmpty() }

  fun filter(predicate: Predicate<String>) =
    copy(matchingRules = matchingRules.filter { predicate.test(it.key) }.toMutableMap())

  fun maxBy(fn: ToIntFunction<String>): MatchingRuleGroup {
    val max = matchingRules.maxBy { fn.applyAsInt(it.key) }
    if (max != null) {
      return max.value
    } else {
      return MatchingRuleGroup()
    }
  }

  fun allMatchingRules() = matchingRules.flatMap { it.value.rules }

  fun addRules(item: String, rules: List<MatchingRule>) {
    if (!matchingRules.containsKey(item)) {
      matchingRules[item] = MatchingRuleGroup(rules.toMutableList())
    } else {
      matchingRules[item]!!.rules.addAll(rules)
    }
  }

  fun applyMatcherRootPrefix(prefix: String) {
    matchingRules = matchingRules.mapKeys { e -> prefix + e.key }.toMutableMap()
  }

  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any?> {
    return if (pactSpecVersion < PactSpecVersion.V3) {
      matchingRules.entries.associate {
        val keyBase = "\$.$name"
        if (it.key.startsWith('$')) {
          Pair(keyBase + it.key.substring(1), it.value.toMap(pactSpecVersion))
        } else {
          Pair(keyBase + it.key, it.value.toMap(pactSpecVersion))
        }
      }
    } else {
      matchingRules.entries.associate { Pair(it.key, it.value.toMap(pactSpecVersion)) }
    }
  }

  fun fromMap(map: Map<String, Any?>) {
    map.forEach { (key, value) ->
      if (value is Map<*, *>) {
        val ruleGroup = MatchingRuleGroup.fromMap(value as Map<String, Any?>)
        if (name == "path") {
          setRules("", ruleGroup)
        } else {
          setRules(key, ruleGroup)
        }
      } else if (name == "path" && value is List<*>) {
        value.forEach {
          addRule(MatchingRuleGroup.ruleFromMap(it as Map<String, Any?>))
        }
      } else {
        logger.warn { "$value is not a valid matcher definition" }
      }
    }
  }
}
