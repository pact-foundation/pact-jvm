package au.com.dius.pact.core.support

fun String?.isNotEmpty(): Boolean = !this.isNullOrEmpty()

fun String?.contains(other: String): Boolean = this?.contains(other, ignoreCase = false) ?: false

fun extractFromMap(json: Map<String, Any>, vararg s: String): Any? {
  return if (s.size == 1) {
    json[s.first()]
  } else if (json.containsKey(s.first())) {
    val map = json[s.first()]
    if (map is Map<*, *>) {
      extractFromMap(map as Map<String, Any>, *s.drop(1).toTypedArray())
    } else {
      null
    }
  } else {
    null
  }
}
