package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

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
}
