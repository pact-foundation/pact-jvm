package au.com.dius.pact.core.model.messaging

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.support.Json
import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.obj
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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

  fun formatContents(): String {
    return if (contents.isPresent()) {
      val mimeType = parseContentType(getContentType())?.mimeType
      when {
        isJson(mimeType) -> Json.gsonPretty.toJson(JsonParser().parse(contents.valueAsString()))
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
    fun fromJson(json: JsonObject): Message {
      val providerStates = when {
        json.has("providerStates") -> json["providerStates"].array.map { ProviderState.fromJson(it) }
        json.has("providerState") -> listOf(ProviderState(Json.toString(json["providerState"])))
        else -> listOf()
      }

      val contents = if (json.has("contents")) {
        val contents = json["contents"]
        when {
          contents.isJsonNull -> OptionalBody.nullBody()
          contents.isJsonPrimitive && contents.asJsonPrimitive.isString ->
            OptionalBody.body(contents.asJsonPrimitive.asString.toByteArray())
          else -> OptionalBody.body(contents.toString().toByteArray())
        }
      } else {
        OptionalBody.missing()
      }
      val matchingRules = if (json.has("matchingRules"))
        MatchingRulesImpl.fromJson(json["matchingRules"])
      else MatchingRulesImpl()
      val generators = if (json.has("generators"))
        Generators.fromJson(json["generators"])
      else Generators()

      val metaData = if (json.has("metaData"))
        json["metaData"].obj.entrySet().associate { it.key to Json.toString(it.value) }
      else
        emptyMap()
      return Message(Json.toString(json["description"]), providerStates,
        contents, matchingRules, generators, metaData)
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
