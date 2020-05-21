package au.com.dius.pact.core.support

import org.apache.commons.lang3.RandomUtils
import java.io.IOException
import java.net.ServerSocket
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

  fun randomPort(lower: Int = 10000, upper: Int = 60000): Int {
    var port: Int? = null
    var count = 0
    while (port == null && count < 20) {
      val randomPort = RandomUtils.nextInt(lower, upper)
      if (portAvailable(randomPort)) {
        port = randomPort
      }
      count++
    }

    return port ?: 0
  }

  fun portAvailable(p: Int): Boolean {
    var socket: ServerSocket? = null
    return try {
      socket = ServerSocket(p)
      true
    } catch (_: IOException) {
      false
    } finally {
      try {
        socket?.close()
      } catch (_: Throwable) { }
    }
  }
}
