package au.com.dius.pact.core.matchers.util

import java.util.Collections.nCopies

fun <E> List<E>.padTo(size: Int, item: E): List<E> {
  return when {
    size < this.size -> subList(fromIndex = 0, toIndex = size)
    size > this.size -> this + nCopies(size - this.size, item)
    else -> this
  }
}
