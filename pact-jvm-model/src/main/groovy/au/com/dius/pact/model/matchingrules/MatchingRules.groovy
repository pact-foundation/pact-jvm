package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Class that encapsulates the matching rules to be applied
 */
@Canonical
class MatchingRules {
  private static final String DOLLAR = '$'
  private static final int TWO = 2

  private final Map rules = [:]

  /**
   * Constructs the matching rules from a Map
   */
  static MatchingRules fromMap(Map map) {
    def matchingRules = new MatchingRules()

    if (map) {
      if (map.keySet().first().startsWith(DOLLAR)) {
        matchingRules.fromV2Map(map)
      } else {
        matchingRules.fromV3Map(map)
      }
    }

    matchingRules
  }

  /**
   * Loads from V3 format matching rules
   */
  void fromV3Map(Map map) {
    map.each {
      addRules(it.key, it.value)
    }
  }

  private void addRules(String categoryName, Map matcherDef) {
    def category = addCategory(categoryName)
    if (categoryName == 'path') {
      matcherDef.matchers.each {
        category.addRule(MatchingRuleUtil.fromMap(it))
      }
    } else {
      matcherDef.each { matcher ->
        matcher.value.matchers.each {
          category.addRule(matcher.key, MatchingRuleUtil.fromMap(it))
        }
      }
    }
  }

  Category addCategory(String category) {
    if (!rules.containsKey(category)) {
      rules[category] = new Category(category)
    }
    rules[category]
  }

  Category addCategory(Category category) {
    rules[category.name] = category
    category
  }

  /**
   * Loads from V2 format matching rules
   */
  void fromV2Map(Map map) {
    map.each {
      if (it.key.startsWith('$.body')) {
        addV2Rule('body', DOLLAR + it.key[6..-1], it.value)
      } else {
        def path = it.key.split('\\.')
        addV2Rule(path[1], path.size() > TWO ? path[TWO] : null, it.value)
      }
    }
  }

  private void addV2Rule(String categoryName, String item, Map matcher) {
    def category = addCategory(categoryName)
    if (item) {
      category.addRule(item, MatchingRuleUtil.fromMap(matcher))
    } else {
      category.addRule(MatchingRuleUtil.fromMap(matcher))
    }
  }

  /**
   * If the rules are empty
   */
  boolean isEmpty() {
    rules.isEmpty()
  }

  /**
   * If the rules are not empty
   */
  boolean isNotEmpty() {
    !isEmpty()
  }

  boolean hasCategory(String category) {
    rules.containsKey(category)
  }

  Set<String> getCategories() {
    rules.keySet()
  }

  Category rulesForCategory(String category) {
    addCategory(category)
  }

  @Override
  String toString() {
    "MatchingRules(rules=$rules)"
  }

  MatchingRules copy() {
    def matchingRules = new MatchingRules()

    rules.each {
      matchingRules.addCategory(it.value.copy())
    }

    matchingRules
  }
}
