package au.com.dius.pact.core.support

import kotlin.reflect.full.cast

object Utils {
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

  fun <T : Any> lookupInMap(map: Map<String, Any?>, key: String, clazz: Class<T>, default: T): T {
    return if (map.containsKey(key)) {
      val value = map[key]
      val valClass = clazz.kotlin
      if (valClass.isInstance(value)) {
        valClass.cast(value)
      } else {
        default
      }
    } else {
      default
    }
  }
}
