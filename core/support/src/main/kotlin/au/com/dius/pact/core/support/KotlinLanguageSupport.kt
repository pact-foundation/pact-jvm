package au.com.dius.pact.core.support

import java.lang.Integer.max

public fun String?.isNotEmpty(): Boolean = !this.isNullOrEmpty()

public fun String?.contains(other: String): Boolean = this?.contains(other, ignoreCase = false) ?: false

public fun <E> List<E>.zipAll(otherList: List<E>): List<Pair<E?, E?>> {
  return (0 until max(this.size, otherList.size)).map {
    this.getOrNull(it) to otherList.getOrNull(it)
  }
}
