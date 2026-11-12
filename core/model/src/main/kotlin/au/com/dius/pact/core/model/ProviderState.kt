package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.Utils.jsonSafeValue
import au.com.dius.pact.core.support.json.JsonValue
import org.apache.commons.lang3.builder.HashCodeBuilder

/**
 * Class that encapsulates all the info about a provider state
 *
 * name - The provider state description
 * params - Provider state parameters as key value pairs
 */
data class ProviderState @JvmOverloads constructor(val name: String?, val params: Map<String, Any?> = mapOf()) {

  fun toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>("name" to name.toString())
    if (params.isNotEmpty()) {
      map["params"] = params.entries.associate {
        it.key to jsonSafeValue(it.value)
      }
    }
    return map
  }

  companion object {
    @JvmStatic
    fun fromJson(json: JsonValue): ProviderState {
      return if (json.has("params") && json["params"] is JsonValue.Object) {
        ProviderState(Json.toString(json["name"]), Json.toMap(json["params"]))
      } else {
        ProviderState(Json.toString(json["name"]))
      }
    }
  }

  fun matches(state: String) = name?.matches(Regex(state)) ?: false

  /**
   * Key used to tell interactions apart when pacts are merged. It includes the parameter values, so interactions
   * that only differ in their provider state parameters are kept. Values are hashed in their JSON form, so the key
   * is stable across runs and stays the same after the state is written to and read back from a pact file.
   */
  fun uniqueKey(): Int {
    val builder = HashCodeBuilder().append(name)
    for (param in params.keys.sorted()) {
      builder.append(param)
      builder.append(Json.toJson(jsonSafeValue(params[param])).serialise())
    }
    return builder.toHashCode()
  }
}
