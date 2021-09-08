package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue

/**
 * Plugin configuration persisted in the pact file metadata
 */
data class PluginData(
  /** Plugin name */
  val name: String,
  /** Plugin version */
  val version: String,
  /** Any configuration supplied by the plugin */
  val configuration: Map<String, Any?>
) {
  companion object {
    fun fromJson(json: JsonValue): PluginData {
      val configuration = when (val config = json["configuration"]) {
        is JsonValue.Object -> Json.fromJson(config) as Map<String, Any?>
        else -> emptyMap()
      }
      return PluginData(Json.toString(json["name"]), Json.toString(json["version"]), configuration)
    }

    fun fromMap(values: Map<String, Any?>): PluginData {
      val configuration = when (val config = values["configuration"]) {
        is Map<*, *> -> config as Map<String, Any?>
        else -> emptyMap()
      }
      return PluginData(values["name"].toString(), values["version"].toString(), configuration)
    }
  }
}
