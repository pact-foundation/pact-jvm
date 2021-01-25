package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.matchingrules.ArrayContainsMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup

class PactDslJsonArrayContaining(
  val root: String,
  rootName: String,
  parent: DslPart
): PactDslJsonArray("", rootName, parent) {
  override fun closeArray(): DslPart {
    val matchers = this.matchers
    this.matchers = MatchingRuleCategory("", mutableMapOf(root + rootName to MatchingRuleGroup(mutableListOf(ArrayContainsMatcher(
      listOf(Triple(
        0,
        matchers.matchingRules.entries.groupBy {
          prefixRegex.find(it.key)?.groups?.get(1)?.toString() ?: ""
        }.map { entry ->
          MatchingRuleCategory("body", entry.value.associate {
            it.key.replace(prefixRegex, "\\$.") to it.value
          }.toMutableMap())
        }.first(),
        generators.categoryFor(Category.BODY)?.entries?.associate {
          it.key.replace(prefixRegex, "\\$.") to it.value
        } ?: emptyMap()
      ))
    )))))
    return super.closeArray()!!
  }

  companion object {
    val prefixRegex = Regex("\\[(\\d+)\\]\\.")
  }
}
