package au.com.dius.pact.model

/**
 * Class that encapsulates all the info about a provider state
 *
 * name - The provider state description
 * params - Provider state parameters as key value pairs
 */
data class ProviderState(val name: String, val params: Map<String, Any> = mapOf()) {

  constructor(name: String?) : this(name ?: "None")

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
      if (map.containsKey("params") && map["params"] is Map<*, *>) {
        return ProviderState(map["name"].toString(), map["params"] as Map<String, Any>)
      } else {
        return ProviderState(map["name"].toString())
      }
    }
  }
}
