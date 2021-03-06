package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.matchingrules.ArrayContainsMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import kotlin.math.max

class PactDslJsonArrayContaining(
  val root: String,
  rootName: String,
  parent: DslPart
): PactDslJsonArray("", rootName, parent) {
  override fun closeArray(): DslPart {
    val groupBy: (Map.Entry<String, Any>) -> Int = {
      val index = prefixRegex.find(it.key)?.groups?.get(1)?.value
      index?.toInt() ?: -1
    }
    val matchingRules = this.matchers.matchingRules.entries.groupBy(groupBy).map { (key, value) ->
      key to MatchingRuleCategory("body", value.associate {
        it.key.replace(prefixRegex, "\\$") to it.value
      }.toMutableMap())
    }
    val generators = generators.categoryFor(Category.BODY)?.entries?.groupBy(groupBy)?.map { (key, value) ->
      key to value.associate {
        it.key.replace(prefixRegex, "\\$") to it.value
      }
    }

    val maxIndex = max(max(matchingRules.maxOf { it.first }, generators?.maxOf { it.first } ?: -1), 0)
    this.matchers = MatchingRuleCategory("", mutableMapOf(root + rootName to MatchingRuleGroup(mutableListOf(ArrayContainsMatcher(
      (0..maxIndex).map { index ->
        Triple(
          index,
          matchingRules.find { it.first == index }?.second ?: MatchingRuleCategory("body"),
          generators?.find { it.first == index }?.second ?: emptyMap()
        )
      }
    )))))
    return super.closeArray()!!
  }

  companion object {
    val prefixRegex = Regex("^\\[(\\d+)]")
  }
}
