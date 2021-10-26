package au.com.dius.pact.core.model.v4

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.json.JsonValue
import mu.KLogging

/**
 * Markup added to an interaction by a plugin
 */
data class InteractionMarkup(
  /**
   * Markup contents
   */
  val markup: String,

  /**
   * Type of markup (CommonMark or HTML)
   */
  val markupType: String
) {
  fun isEmpty() = markup.isEmpty()

  fun isNotEmpty() = markup.isNotEmpty()

  fun toMap() = mapOf(
    "markup" to markup,
    "markupType" to markupType
  )

  /**
   * Merges this markup with the other
   */
  fun merge(other: InteractionMarkup): InteractionMarkup {
    return if (this.isEmpty()) {
      other
    } else if (other.isEmpty()) {
      this
    } else {
      if (this.markupType != other.markupType) {
        logger.warn { "Merging different markup types: ${this.markupType} and ${other.markupType}" }
      }
      InteractionMarkup(this.markup + "\n" + other.markup, this.markupType)
    }
  }

  companion object : KLogging() {
    fun fromJson(json: JsonValue): InteractionMarkup {
      return when (json) {
        is JsonValue.Object -> InteractionMarkup(Json.toString(json["markup"]), Json.toString(json["markupType"]))
        else -> {
          logger.warn { "$json is not valid for InteractionMarkup" }
          InteractionMarkup("", "")
        }
      }
    }

  }
}
