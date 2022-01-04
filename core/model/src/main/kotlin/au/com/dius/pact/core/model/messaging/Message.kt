package au.com.dius.pact.core.model.messaging

import au.com.dius.pact.core.model.BaseInteraction
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonException
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.core.support.json.KafkaSchemaRegistryJsonParser
import mu.KLogging
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.StringUtils

interface MessageInteraction {
  val description: String
  val interactionId: String?
}

/**
 * Message in a Message Pact
 */
class Message @JvmOverloads constructor(
  description: String,
  providerStates: List<ProviderState> = listOf(),
  var contents: OptionalBody = OptionalBody.missing(),
  val matchingRules: MatchingRules = MatchingRulesImpl(),
  var generators: Generators = Generators(),
  var metadata: MutableMap<String, Any?> = mutableMapOf(),
  interactionId: String? = null
) : BaseInteraction(interactionId, description, providerStates), MessageInteraction {

  fun contentsAsBytes() = contents.orEmpty()

  fun contentsAsString() = contents.valueAsString()

  fun getContentType() = contentType(metadata).or(contents.contentType)

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any?> {
    val map: MutableMap<String, Any?> = mutableMapOf(
      "description" to description,
      "metaData" to metadata
    )
    if (!contents.isMissing()) {
      map["contents"] = when {
        isJsonContents() -> {
          try {
            val json = JsonParser.parseString(contents.valueAsString())
            if (json is JsonValue.StringValue) {
              contents.valueAsString()
            } else {
              Json.fromJson(json)
            }
          } catch (ex: JsonException) {
            logger.trace(ex) { "Failed to parse JSON body" }
            contents.valueAsString()
          }
        }
        isKafkaSchemaRegistryCompliantJsonContents() -> {
          try {
            val json = KafkaSchemaRegistryJsonParser.parseString(contents.valueAsString())
            Json.fromJson(json)
          } catch (ex: JsonException) {
            logger.trace(ex) { "Failed to parse Kafka Schema Registry Compliant JSON body" }
            contents.valueAsString()
          }
        }
        else -> formatContents()
      }
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

  private fun isJsonContents(): Boolean {
    return if (contents.isPresent()) {
      contentType(metadata).or(contents.contentType).isJson()
    } else {
      false
    }
  }

  private fun isKafkaSchemaRegistryCompliantJsonContents(): Boolean {
    return if (contents.isPresent()) {
      contentType(metadata).or(contents.contentType).isKafkaSchemaRegistryJson()
    } else {
      false
    }
  }

  fun formatContents(): String {
    return if (contents.isPresent()) {
      val contentType = contentType(metadata).or(contents.contentType)
      when {
        contentType.isKafkaSchemaRegistryJson() -> KafkaSchemaRegistryJsonParser.parseString(contents.valueAsString()).prettyPrint()
        contentType.isJson() -> JsonParser.parseString(contents.valueAsString()).prettyPrint()
        contentType.isOctetStream() -> Base64.encodeBase64String(contentsAsBytes())
        else -> contents.valueAsString()
      }
    } else {
      ""
    }
  }

  override fun uniqueKey(): String {
    return StringUtils.defaultIfEmpty(providerStates.joinToString { it.name.toString() }, "None") +
      "_$description"
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
      "matchingRules=$matchingRules, generators=$generators, metadata=$metadata)"
  }

  fun withMetaData(metadata: Map<String, Any>): Message {
    this.metadata = metadata.toMutableMap()
    return this
  }

  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    val errors = mutableListOf<String>()
    errors.addAll(matchingRules.validateForVersion(pactVersion))
    errors.addAll(generators.validateForVersion(pactVersion))
    return errors
  }

  @ExperimentalUnsignedTypes
  override fun asV4Interaction(): V4Interaction {
    return V4Interaction.AsynchronousMessage("", description, MessageContents(contents, metadata,
      matchingRules.rename("body", "content"), generators),
      interactionId, providerStates).withGeneratedKey()
  }

  override fun isAsynchronousMessage() = true

  override fun asMessage() = this

  companion object : KLogging() {

    /**
     * Builds a message from a Map
     */
    @JvmStatic
    fun fromJson(json: JsonValue.Object): Message {
      val providerStates = when {
        json.has("providerStates") -> json["providerStates"].asArray()?.values?.map { ProviderState.fromJson(it) }
        json.has("providerState") -> listOf(ProviderState(Json.toString(json["providerState"])))
        else -> listOf()
      }

      val metaData = if (json.has("metaData") && json["metaData"].isObject)
        json["metaData"].asObject()!!.entries.entries.associate { it.key to Json.fromJson(it.value) }
      else
        emptyMap()

      val contentType = contentType(metaData)
      val contents = if (json.has("contents")) {
        when (val contents = json["contents"]) {
          is JsonValue.Null -> OptionalBody.nullBody()
          is JsonValue.StringValue -> OptionalBody.body(contents.asString()!!.toByteArray(contentType.asCharset()),
            contentType)
          else -> OptionalBody.body(contents.serialise().toByteArray(contentType.asCharset()), contentType)
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

      return Message(Json.toString(json["description"]), providerStates ?: emptyList(),
        contents, matchingRules, generators, metaData.toMutableMap(), Json.toString(json["_id"]))
    }

    fun contentType(metaData: Map<String, Any?>): ContentType {
      return ContentType.fromString(metaData.entries.find {
        it.key.toLowerCase() == "contenttype" || it.key.toLowerCase() == "content-type"
      }?.value?.toString())
    }
  }
}
