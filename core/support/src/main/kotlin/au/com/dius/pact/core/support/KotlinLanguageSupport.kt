package au.com.dius.pact.core.support

import java.lang.Integer.max
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

public fun String?.isNotEmpty(): Boolean = !this.isNullOrEmpty()

public fun String?.contains(other: String): Boolean = this?.contains(other, ignoreCase = false) ?: false

public fun <E> List<E>.zipAll(otherList: List<E>): List<Pair<E?, E?>> {
  return (0 until max(this.size, otherList.size)).map {
    this.getOrNull(it) to otherList.getOrNull(it)
  }
}

public fun Any?.hasProperty(name: String) = this != null && this::class.memberProperties.any { it.name == name }

public fun Any?.property(name: String) = if (this != null) {
  this::class.memberProperties.find { it.name == name } as KProperty1<Any, Any?>?
} else null
