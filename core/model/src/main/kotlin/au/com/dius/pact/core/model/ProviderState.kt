package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.toMap
import com.google.gson.JsonElement

/**
 * Class that encapsulates all the info about a provider state
 *
 * name - The provider state description
 * params - Provider state parameters as key value pairs
 */
data class ProviderState @JvmOverloads constructor(val name: String, val params: Map<String, Any?> = mapOf()) {

  fun toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>("name" to name)
    if (params.isNotEmpty()) {
      map["params"] = params
    }
    return map
  }

  companion object {
    @JvmStatic
    fun fromJson(json: JsonElement): ProviderState {
      return if (json.obj.has("params") && json.obj["params"].isJsonObject) {
        ProviderState(Json.toString(json.obj["name"]), Json.toMap(json.obj["params"].obj))
      } else {
        ProviderState(Json.toString(json.obj["name"]))
      }
    }
  }

  fun matches(state: String) = name.matches(Regex(state))
}
