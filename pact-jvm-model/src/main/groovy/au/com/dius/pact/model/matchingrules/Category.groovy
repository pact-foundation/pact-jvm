package au.com.dius.pact.model.matchingrules

import au.com.dius.pact.model.PactSpecVersion
import groovy.transform.Canonical

import java.util.function.Predicate
import java.util.function.ToIntFunction

/**
 * Matching rules category
 */
@Canonical
class Category {
  String name
  Map<String, List<MatchingRule>> matchingRules = [:]
  RuleLogic ruleLogic = RuleLogic.AND

  void addRule(String item, MatchingRule matchingRule) {
    if (!matchingRules.containsKey(item)) {
      matchingRules[item] = []
    }
    matchingRules[item] << matchingRule
  }

  void addRule(MatchingRule matchingRule) {
    addRule('', matchingRule)
  }

  void setRule(String item, MatchingRule matchingRule) {
    matchingRules[item] = [ matchingRule ]
  }

  void setRule(MatchingRule matchingRule) {
    setRule('', matchingRule)
  }

  /**
   * If the rules are empty
   */
  boolean isEmpty() {
    matchingRules.every { it.value.empty }
  }

  /**
   * If the rules are not empty
   */
  boolean isNotEmpty() {
    matchingRules.any { !it.value.empty }
  }

  Category filter(Predicate<String> predicate) {
    new Category(name, matchingRules.findAll { predicate.test(it.key) }, ruleLogic)
  }

  Category maxBy(ToIntFunction<String> fn) {
    def map = matchingRules.max { k, v -> fn(k) }
    new Category(name, [(map.key): map.value], ruleLogic)
  }

  List<MatchingRule> allMatchingRules() {
    matchingRules.values().flatten()
  }

  void addRules(String item, List<MatchingRule> rules) {
    if (!matchingRules.containsKey(item)) {
      matchingRules[item] = []
    }
    matchingRules[item].addAll(rules)
  }

  void applyMatcherRootPrefix(String prefix) {
    matchingRules = matchingRules.collectEntries {
      [prefix + it.key, it.value]
    }
  }

  Category copy() {
    new Category(name, [:] + matchingRules, ruleLogic)
  }

  Map toMap(PactSpecVersion pactSpecVersion) {
    if (pactSpecVersion < PactSpecVersion.V2) {
      matchingRules.collectEntries {
        [it.key.replaceFirst('^\\$', '$.' + name), it.value.first().toMap()]
      }
    } else {
      matchingRules.collectEntries {
        [it.key, [matchers: it.value*.toMap()]]
      }
    }
  }
}
