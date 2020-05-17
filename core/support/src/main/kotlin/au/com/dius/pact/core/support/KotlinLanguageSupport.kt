package au.com.dius.pact.core.support

import java.lang.Integer.max
import java.net.URL
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import arrow.core.Either
import java.lang.RuntimeException

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

public fun String?.toUrl() = if (this.isNullOrEmpty()) {
  null
} else {
  URL(this)
}

public fun <F> handleWith(f: () -> Any): Either<Exception, F> {
  return try {
    val result = f()
    if (result is Either<*, *>) result as Either<Exception, F> else Either.right(result as F)
  } catch (ex: Exception) {
    Either.left(ex)
  }
}

public fun <A> Either<Exception, A>.unwrapOr(v: A): A {
  return when (this) {
    is Either.Left -> v
    is Either.Right -> this.b
  }
}

public fun <A, B> Either<A, B>.unwrap(): B {
  when (this) {
    is Either.Left -> when (a) {
      is Throwable -> throw a as Throwable
      else -> throw RuntimeException(a.toString())
    }
    is Either.Right -> return b
  }
}
