package au.com.dius.pact.core.support

import mu.KLogging
import org.apache.commons.lang3.RandomUtils
import java.io.IOException
import java.net.ServerSocket
import java.util.jar.JarInputStream
import kotlin.reflect.full.cast
import kotlin.reflect.full.declaredMemberProperties

/**
 * Common utility functions
 */
object Utils : KLogging() {
  /**
   * Recursively extracts a sequence of keys from a recursive Map structure
   */
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

  /**
   * Looks up a key in a map of a particular type. If the key does not exist, or the value is not the correct type,
   * returns the default value
   */
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

  /**
   * Finds a random open port between the min and max port values
   */
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

  /**
   * Determines if the given port number is available. Does this by trying to open a socket and then immediately closing it.
   */
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

  /**
   * Returns a list of pairs of all the permutations of combining two lists
   */
  fun <T1, T2> permutations(list1: List<T1>, list2: List<T2>): List<Pair<T1?, T2?>> {
    val result = mutableListOf<Pair<T1?, T2?>>()
    if (list1.isNotEmpty() || list2.isNotEmpty()) {
      val firstList = list1.ifEmpty { listOf<T1?>(null) }
      val secondList = list2.ifEmpty { listOf<T2?>(null) }
      for (item1 in firstList) {
        for (item2 in secondList) {
          result.add(item1 to item2)
        }
      }
    }
    return result
  }

  /**
   * Recursively converts an object properties into a map structure
   */
  fun objectToJsonMap(obj: Any?): Map<String, Any?>? {
    return if (obj != null) {
      obj::class.declaredMemberProperties.associate { prop ->
        val key = prop.name
        val value = prop.getter.call(obj)
        key to jsonSafeValue(value)
      }
    } else {
      null
    }
  }

  /**
   * Ensures a value is safe to be converted into JSON
   */
  fun jsonSafeValue(value: Any?): Any? {
    return if (value != null) {
      when (value) {
        is Boolean -> value
        is String -> value
        is Number -> value
        is Map<*, *> -> value.entries.associate { it.key.toString() to jsonSafeValue(it.value) }
        is Collection<*> -> value.map { jsonSafeValue(it) }
        else -> objectToJsonMap(value)
      }
    } else {
      null
    }
  }

  /**
   * Tries to lookup the version of the library that invoked this method by accessing the Implementation-Version
   * from the Jar manifest
   */
  fun lookupVersion(clazz: Class<*>): String {
    val url = clazz.protectionDomain?.codeSource?.location
    return if (url != null) {
      val openStream = url.openStream()
      try {
        val jarStream = JarInputStream(openStream)
        jarStream.manifest?.mainAttributes?.getValue("Implementation-Version") ?: ""
      } catch (e: IOException) {
        logger.warn(e) { "Could not load pact-jvm manifest" }
        ""
      } finally {
        openStream.close()
      }
    } else {
      ""
    }
  }

  /**
   * Looks up a value from the environment, first by looking for the JVM system property with the key, then
   * looking for an environment variable with the key, then looking for the snake-cased version of the key as an
   * environment variable.
   */
  @JvmOverloads
  fun lookupEnvironmentValue(
    key: String,
    sysLookup: (key: String) -> String? = System::getProperty,
    envLookup: (key: String) -> String? = System::getenv
  ): String? {
    var value: String? = sysLookup(key)
    if (value.isNullOrEmpty()) {
      value = envLookup(key)
    }
    if (value.isNullOrEmpty()) {
      value = envLookup(snakeCase(key))
    }
    return value
  }

  /**
   * Convert a value to snake-case form (a.b.c -> A_B_C)
   */
  private fun snakeCase(key: String) = key.split('.').joinToString("_") { it.toUpperCase() }
}
