package au.com.dius.pact.core.model.messaging

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import mu.KLogging
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.StringUtils
import org.apache.http.entity.ContentType

/**
 * Message in a Message Pact
 */
class Message @JvmOverloads constructor(
  override val description: String,
  override val providerStates: List<ProviderState> = listOf(),
  var contents: OptionalBody = OptionalBody.missing(),
  var matchingRules: MatchingRules = MatchingRulesImpl(),
  var generators: Generators = Generators(),
  var metaData: Map<String, String> = mapOf()
): Interaction {

  fun contentsAsBytes() = contents.orEmpty()

  fun contentsAsString() = contents.valueAsString()

  fun getContentType(): String {
    return metaData.entries.find {
      it.key.toLowerCase() == "contenttype" || it.key.toLowerCase() == "content-type"
    }?.value ?: JSON
  }

  fun getParsedContentType() = parseContentType(this.getContentType())

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    val map = mutableMapOf(
      "description" to description,
      "metaData" to metaData
    )
    if (!contents.isMissing()) {
      map["contents"] = formatContents()
    }
    if (providerStates.isNotEmpty()) {
      map["providerStates"] = providerStates.map { it.toMap() }
    }
    if (matchingRules.isNotEmpty()) {
      map["matchingRules"] = matchingRules.toMap(pactSpecVersion)
    }
    if (generators.isNotEmpty()) {
      map["generators"] = generators.toMap(pactSpecVersion)
    }
    return map
  }

  fun formatContents(): Any {
    return if (contents.isPresent()) {
      val mimeType = parseContentType(getContentType())?.mimeType
      when {
        isJson(mimeType) -> JsonSlurper().parseText(contents.valueAsString())
        isOctetStream(mimeType) -> Base64.encodeBase64String(contentsAsBytes())
        else -> contents.valueAsString()
      }
    } else {
      ""
    }
  }

  override fun uniqueKey(): String {
    return "${StringUtils.defaultIfEmpty(providerStates.joinToString { it.name }, "None")}_$description"
  }

  override fun conflictsWith(other: Interaction) = other !is Message

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Message

    if (description != other.description) return false
    if (providerStates != other.providerStates) return false
    if (contents != other.contents) return false
    if (matchingRules != other.matchingRules) return false
    if (generators != other.generators) return false

    return true
  }

  override fun hashCode(): Int {
    var result = description.hashCode()
    result = 31 * result + providerStates.hashCode()
    result = 31 * result + contents.hashCode()
    result = 31 * result + matchingRules.hashCode()
    result = 31 * result + generators.hashCode()
    return result
  }

  override fun toString(): String {
    return "Message(description='$description', providerStates=$providerStates, contents=$contents, " +
      "matchingRules=$matchingRules, generators=$generators, metaData=$metaData)"
  }

  companion object: KLogging() {
    const val JSON = "application/json"

    /**
     * Builds a message from a Map
     */
    @JvmStatic
    fun fromMap(map: Map<String, Any>): Message {
      val providerStates = when {
        map.containsKey("providerStates") -> (map["providerStates"] as List<Map<String, Any>>).map { ProviderState.fromMap(it) }
        map.containsKey("providerState") -> listOf(ProviderState(map["providerState"].toString()))
        else -> listOf()
      }

      val contents = if (map.containsKey("contents")) {
        val contents = map["contents"]
        if (contents == null) {
          OptionalBody.nullBody()
        } else if (contents is String && contents.isEmpty()) {
          OptionalBody.empty()
        } else {
          OptionalBody.body(JsonOutput.toJson(contents).toByteArray())
        }
      } else {
        OptionalBody.missing()
      }
      val matchingRules = if (map.containsKey("matchingRules"))
        MatchingRulesImpl.fromMap(map["matchingRules"] as Map<String, Map<String, Any?>>)
      else MatchingRulesImpl()
      val generators = if (map.containsKey("generators"))
        Generators.fromMap(map["generators"] as Map<String, Map<String, Any>>)
      else Generators()

      return Message(map.getOrDefault("description", "").toString(), providerStates,
        contents, matchingRules, generators, map["metaData"] as Map<String, String>? ?: emptyMap())
    }

    private fun parseContentType(contentType: String): ContentType? {
      return try {
        ContentType.parse(contentType)
      } catch (e: RuntimeException) {
        logger.debug(e) { "Failed to parse content type '$contentType'" }
        null
      }
    }

    private fun isJson(contentType: String?) =
      contentType != null && contentType.matches(Regex("application/.*json"))

    private fun isOctetStream(contentType: String?) = contentType == "application/octet-stream"
  }
}
