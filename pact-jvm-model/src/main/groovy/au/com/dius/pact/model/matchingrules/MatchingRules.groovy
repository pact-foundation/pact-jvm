package au.com.dius.pact.model.matchingrules

import au.com.dius.pact.model.PactSpecVersion
import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * Class that encapsulates the matching rules to be applied
 */
@Canonical
class MatchingRules {
  private static final String DOLLAR = '$'
  private static final int TWO = 2
  private static final String DOLLAR_BODY = '$.body'
  private static final String BODY = 'body'

  private final Map<String, Category> rules = [:]

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
    addCategory(categoryName).fromMap(matcherDef)
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
      def path = it.key.split('\\.')
      if (it.key.startsWith(DOLLAR_BODY)) {
        if (it.key == DOLLAR_BODY) {
          addV2Rule(BODY, DOLLAR, it.value)
        } else {
          addV2Rule(BODY, DOLLAR + it.key[6..-1], it.value)
        }
      } else if (it.key.startsWith('$.headers')) {
        addV2Rule('header', path[TWO], it.value)
      } else {
        addV2Rule(path[1], path.size() > TWO ? path[TWO] : null, it.value)
      }
    }
  }

  @CompileStatic
  private void addV2Rule(String categoryName, String item, Map<String, Object> matcher) {
    def category = addCategory(categoryName)
    if (item) {
      category.addRule(item, MatchingRuleGroup.ruleFromMap(matcher))
    } else {
      category.addRule(MatchingRuleGroup.ruleFromMap(matcher))
    }
  }

  /**
   * If the rules are empty
   */
  boolean isEmpty() {
    rules.every { it.value.empty }
  }

  /**
   * If the rules are not empty
   */
  boolean isNotEmpty() {
    rules.any { it.value.notEmpty }
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
      matchingRules.addCategory(it.value /*.copy()*/)
    }

    matchingRules
  }

  Map toMap(PactSpecVersion pactSpecVersion) {
    if (pactSpecVersion < PactSpecVersion.V3) {
      toV2Map()
    } else {
      toV3Map()
    }
  }

  Map toV3Map() {
    def map = [:]

    rules.each {
      map[it.key] = it.value.toMap(PactSpecVersion.V3)
    }

    map
  }

  Map toV2Map() {
    def map = [:]

    rules.each {
      it.value.toMap(PactSpecVersion.V2).each {
        map[it.key] = it.value
      }
    }

    map
  }
}
