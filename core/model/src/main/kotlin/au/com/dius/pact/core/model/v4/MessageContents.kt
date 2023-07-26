package au.com.dius.pact.core.model.v4

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.bodyFromJson
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.support.json.JsonValue
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Contents of a message interaction
 */
data class MessageContents @JvmOverloads constructor(
  val contents: OptionalBody = OptionalBody.missing(),
  val metadata: MutableMap<String, Any?> = mutableMapOf(),
  val matchingRules: MatchingRules = MatchingRulesImpl(),
  val generators: Generators = Generators(),
  val partName: String = ""
) {
  fun getContentType() = contents.contentType.or(Message.contentType(metadata))

  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, *> {
    val map = mutableMapOf(
      "contents" to contents.toV4Format()
    )
    if (metadata.isNotEmpty()) {
      map["metadata"] = metadata
    }
    if (matchingRules.isNotEmpty()) {
      map["matchingRules"] = matchingRules.toMap(pactSpecVersion)
    }
    if (generators.isNotEmpty()) {
      map["generators"] = generators.toMap(pactSpecVersion)
    }
    return map
  }

  override fun toString(): String {
    return "Message Contents ( contents: $contents, metadata: $metadata )"
  }

  /**
   * Configures any generators for the given category
   */
  fun setupGeneratorsFor(category: Category, context: MutableMap<String, Any>): Map<String, Generator> {
    val generators = generators.categories[category] ?: emptyMap()
    val matchingRuleGenerators = matchingRules.rulesForCategory(category.name.lowercase()).generators(context)
    return generators + matchingRuleGenerators
  }

  companion object {

    private val logger = KotlinLogging.logger {}
    fun fromJson(json: JsonValue): MessageContents {
      val metadata = if (json.has("metadata")) {
        val jsonValue = json["metadata"]
        if (jsonValue is JsonValue.Object) {
          jsonValue.entries
        } else {
          logger.warn { "Ignoring invalid message metadata ${jsonValue.serialise()}" }
          mapOf()
        }
      } else {
        mapOf()
      }
      val contents = bodyFromJson("contents", json, metadata)
      val matchingRules = if (json.has("matchingRules") && json["matchingRules"] is JsonValue.Object)
        MatchingRulesImpl.fromJson(json["matchingRules"])
      else MatchingRulesImpl()
      val generators = if (json.has("generators") && json["generators"] is JsonValue.Object)
        Generators.fromJson(json["generators"])
        else Generators()
      return MessageContents(contents, metadata.toMutableMap(), matchingRules, generators)
    }
  }
}
