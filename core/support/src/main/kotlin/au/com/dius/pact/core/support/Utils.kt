package au.com.dius.pact.core.support

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import mu.KLogging
import org.apache.commons.lang3.RandomUtils
import java.io.IOException
import java.net.ServerSocket
import java.util.jar.JarInputStream
import kotlin.math.pow
import kotlin.reflect.full.cast
import kotlin.reflect.full.declaredMemberProperties

object Utils : KLogging() {
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

  fun <T1, T2> permutations(list1: List<T1>, list2: List<T2>): List<Pair<T1?, T2?>> {
    val result = mutableListOf<Pair<T1?, T2?>>()
    if (list1.isNotEmpty() || list2.isNotEmpty()) {
      val firstList = if (list1.isEmpty()) listOf<T1?>(null) else list1
      val secondList = if (list2.isEmpty()) listOf<T2?>(null) else list2
      for (item1 in firstList) {
        for (item2 in secondList) {
          result.add(item1 to item2)
        }
      }
    }
    return result
  }

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

  private val SIZE_REGEX = Regex("(\\d+)(\\w+)")
  private val DATA_SIZES = listOf("b", "kb", "mb", "gb", "tb")

  fun sizeOf(value: String): Result<Int, String> {
    val matchResult = SIZE_REGEX.matchEntire(value.toLowerCase())
    return if (matchResult != null) {
      val unitPower = DATA_SIZES.indexOf(matchResult.groupValues[2])
      if (unitPower >= 0) {
        Ok(Integer.parseInt(matchResult.groupValues[1]) * 1024.0.pow(unitPower).toInt())
      } else {
        Err("'$value' is not a valid data size")
      }
    } else {
      Err("'$value' is not a valid data size")
    }
  }
}
