package au.com.dius.pact.core.model

/**
 * Class that encapsulates all the info about a provider state
 *
 * name - The provider state description
 * params - Provider state parameters as key value pairs
 */
data class ProviderState @JvmOverloads constructor(val name: String, val params: Map<String, Any> = mapOf()) {

  fun toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>("name" to name)
    if (params.isNotEmpty()) {
      map["params"] = params
    }
    return map
  }

  companion object {
    @JvmStatic
    fun fromMap(map: Map<String, Any>): ProviderState {
      return if (map.containsKey("params") && map["params"] is Map<*, *>) {
        ProviderState(map["name"].toString(), map["params"] as Map<String, Any>)
      } else {
        ProviderState(map["name"].toString())
      }
    }
  }

  fun matches(state: String) = name.matches(Regex(state))
}
