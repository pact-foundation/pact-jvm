package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.support.json.JsonValue
import mu.KLogging
import java.util.Comparator
import java.util.function.Predicate

/**
 * Matching rules category
 */
data class MatchingRuleCategory @JvmOverloads constructor(
  val name: String,
  var matchingRules: MutableMap<String, MatchingRuleGroup> = mutableMapOf()
) {

  companion object : KLogging()

  /**
   * Add a rule by key to the given category
   */
  @JvmOverloads
  fun addRule(item: String, matchingRule: MatchingRule, ruleLogic: RuleLogic = RuleLogic.AND): MatchingRuleCategory {
    if (!matchingRules.containsKey(item)) {
      matchingRules[item] = MatchingRuleGroup(mutableListOf(matchingRule), ruleLogic)
    } else {
      matchingRules[item]!!.rules.add(matchingRule)
    }
    return this
  }

  /**
   * Add a non-key rule to the given category
   */
  @JvmOverloads
  fun addRule(matchingRule: MatchingRule, ruleLogic: RuleLogic = RuleLogic.AND) =
    addRule("", matchingRule, ruleLogic)

  /**
   * Sets rule at the given key
   */
  @JvmOverloads
  fun setRule(item: String, matchingRule: MatchingRule, ruleLogic: RuleLogic = RuleLogic.AND) {
    matchingRules[item] = MatchingRuleGroup(mutableListOf(matchingRule), ruleLogic)
  }

  /**
   * Sets a non-key rule
   */
  @JvmOverloads
  fun setRule(matchingRule: MatchingRule, ruleLogic: RuleLogic = RuleLogic.AND) =
    setRule("", matchingRule, ruleLogic)

  /**
   * Sets all the rules to the provided key
   */
  @JvmOverloads
  fun setRules(item: String, rules: List<MatchingRule>, ruleLogic: RuleLogic = RuleLogic.AND) {
    setRules(item, MatchingRuleGroup(rules.toMutableList(), ruleLogic))
  }

  /**
   * Sets all the rules as non-keyed rules
   */
  @JvmOverloads
  fun setRules(matchingRules: List<MatchingRule>, ruleLogic: RuleLogic = RuleLogic.AND) =
    setRules("", matchingRules, ruleLogic)

  /**
   * Sets the matching rule group at the provided key
   */
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

  /**
   * Returns a new Category filtered by the predicate
   */
  fun filter(predicate: Predicate<String>) =
    copy(matchingRules = matchingRules.filter { predicate.test(it.key) }.toMutableMap())

  /**
   * Returns a new Category filtered by the predicate
   */
  fun filter2(predicate: Predicate<Pair<String,MatchingRuleGroup>>) =
    copy(matchingRules = matchingRules.filter { predicate.test(it.key to it.value) }.toMutableMap())

  fun maxBy(comparator: Comparator<String>): Pair<String, MatchingRuleGroup>? {
    val max = matchingRules.entries.fold(matchingRules.entries.firstOrNull()) { acc, entry ->
      if (acc != null && comparator.compare(acc.key, entry.key) >= 0) {
        acc
      } else {
        entry
      }
    }
    return max?.toPair()
  }

  /**
   * Returns all the matching rules
   */
  fun allMatchingRules() = matchingRules.flatMap { it.value.rules }

  /**
   * Adds all the rules to the given key
   */
  @JvmOverloads
  fun addRules(item: String, rules: List<MatchingRule>, ruleLogic: RuleLogic = RuleLogic.AND) {
    if (!matchingRules.containsKey(item)) {
      matchingRules[item] = MatchingRuleGroup(rules.toMutableList(), ruleLogic)
    } else {
      matchingRules[item]!!.rules.addAll(rules)
    }
  }

  /**
   * Re-key all the rules with the given prefix
   */
  fun applyMatcherRootPrefix(prefix: String) {
    matchingRules = matchingRules.mapKeys { e ->
      when {
        e.key.startsWith("$") -> prefix + e.key.substring(1)
        else -> prefix + e.key
      }
    }.toMutableMap()
  }

  /**
   * Create a copy of the category with all rules re-keyed with the prefix
   */
  fun copyWithUpdatedMatcherRootPrefix(prefix: String): MatchingRuleCategory {
    val category = copy()
    category.applyMatcherRootPrefix(prefix)
    return category
  }

  /**
   * Serialise this category to a Map
   */
  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any?> {
    return if (pactSpecVersion < PactSpecVersion.V3) {
      matchingRules.entries.associate {
        val keyBase = when (name) {
          "header" -> "\$.headers"
          else -> "\$.$name"
        }
        val key = when {
          it.key.startsWith('$') -> keyBase + it.key.substring(1)
          it.key.isNotEmpty() && !it.key.startsWith('[') -> keyBase + '.' + it.key
          it.key.isNotEmpty() -> keyBase + it.key
          else -> keyBase
        }
        Pair(key, it.value.toMap(pactSpecVersion))
      }
    } else {
      matchingRules.flatMap { entry ->
        if (entry.key.isEmpty()) {
          entry.value.toMap(pactSpecVersion).entries.map { it.toPair() }
        } else {
          listOf(entry.key to entry.value.toMap(pactSpecVersion))
        }
      }.toMap()
    }
  }

  /**
   * Deserialise the category from the Map
   */
  @Deprecated("Use fromJson", replaceWith = ReplaceWith("fromJson"))
  fun fromMap(map: Map<String, Any?>): MatchingRuleCategory {
    if (categoryRequiresSubkeys()) {
      map.forEach { (key, value) ->
        if (value is Map<*, *>) {
          val ruleGroup = MatchingRuleGroup.fromMap(value as Map<String, Any?>)
          setRules(key, ruleGroup)
        } else if (name == "path" && value is List<*>) {
          value.forEach {
            addRule(MatchingRuleGroup.ruleFromMap(it as Map<String, Any?>))
          }
        } else {
          logger.warn { "$value is not a valid matcher definition" }
        }
      }
    } else {
      if (map.size == 1 && map.containsKey("")) {
        // This is due to Defect #743
        setRules("", MatchingRuleGroup.fromMap(map[""] as Map<String, Any?>))
      } else {
        setRules("", MatchingRuleGroup.fromMap(map))
      }
    }
    return this
  }

  /**
   * Deserialise the category from JSON
   */
  fun fromJson(matcherDef: JsonValue): MatchingRuleCategory {
    if (matcherDef is JsonValue.Object) {
      if (categoryRequiresSubkeys()) {
        matcherDef.entries.forEach { (key, value) ->
          if (value is JsonValue.Object) {
            val ruleGroup = MatchingRuleGroup.fromJson(value)
            setRules(key, ruleGroup)
          } else if (name == "path" && value is JsonValue.Array) {
            value.values.forEach {
              addRule(MatchingRule.fromJson(it))
            }
          } else {
            logger.warn { "$value is not a valid matcher definition" }
          }
        }
      } else {
        val map = matcherDef.entries
        if (map.size == 1 && map.containsKey("")) {
          // This is due to Defect #743
          setRules("", MatchingRuleGroup.fromJson(matcherDef[""]))
        } else {
          setRules("", MatchingRuleGroup.fromJson(matcherDef))
        }
      }
    }
    return this
  }

  private fun categoryRequiresSubkeys() = name != "path" && name != "status"

  /**
   * Returns the number of rules stored at the key
   */
  fun numRules(key: String) = matchingRules.getOrDefault(key, MatchingRuleGroup()).rules.size

  /** Validates all the rules in this category against the Pact specification version */
  fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return matchingRules.values.flatMap { it.validateForVersion(pactVersion) }
  }

  fun generators(context: Map<String, Any>): Map<String, Generator> {
    val map = mutableMapOf<String, Generator>()
    for (entry in matchingRules) {
      for (rule in entry.value.rules) {
        if (rule.hasGenerators()) {
          for (generator in rule.buildGenerators(context)) {
            map[entry.key] = generator
          }
        }
      }
    }
    return map
  }
}
