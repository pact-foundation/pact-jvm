package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.model.PactSpecVersion
import mu.KLogging
import java.util.Comparator
import java.util.function.Predicate
import java.util.function.ToIntFunction

/**
 * Matching rules category
 */
data class Category @JvmOverloads constructor(
  val name: String,
  var matchingRules: MutableMap<String, MatchingRuleGroup> = mutableMapOf()
) {

  companion object : KLogging()

  fun addRule(item: String, matchingRule: MatchingRule): Category {
    if (!matchingRules.containsKey(item)) {
      matchingRules[item] = MatchingRuleGroup(mutableListOf(matchingRule))
    } else {
      matchingRules[item]!!.rules.add(matchingRule)
    }
    return this
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

  @Deprecated("Use maxBy(Comparator) as this function causes a defect (see issue #698)")
  fun maxBy(fn: ToIntFunction<String>): MatchingRuleGroup {
    val max = matchingRules.maxBy { fn.applyAsInt(it.key) }
    return max?.value ?: MatchingRuleGroup()
  }

  fun maxBy(comparator: Comparator<String>): MatchingRuleGroup {
    val max = matchingRules.maxWith(Comparator { a, b -> comparator.compare(a.key, b.key) })
    return max?.value ?: MatchingRuleGroup()
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

  fun copyWithUpdatedMatcherRootPrefix(prefix: String): Category {
    val category = copy()
    category.applyMatcherRootPrefix(prefix)
    return category
  }

  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any?> {
    return if (pactSpecVersion < PactSpecVersion.V3) {
      matchingRules.entries.associate {
        val keyBase = "\$.$name"
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
