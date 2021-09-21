package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.json.JsonValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.UnwrapException
import java.lang.Integer.max
import java.net.URL
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

public fun String?.isNotEmpty(): Boolean = !this.isNullOrEmpty()

public fun String?.contains(other: String): Boolean = this?.contains(other, ignoreCase = false) ?: false

public fun List<*>?.isNotEmpty(): Boolean = !this.isNullOrEmpty()

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

public fun <F> handleWith(f: () -> Any?): Result<F, Exception> {
  return try {
    val result = f()
    if (result is Result<*, *>) result as Result<F, Exception> else Ok(result as F)
  } catch (ex: Exception) {
    Err(ex)
  } catch (ex: Throwable) {
    Err(RuntimeException(ex))
  }
}

public fun <A, B> Result<A, B>.unwrap(): A {
  when (this) {
    is Err<*> -> when (error) {
      is Throwable -> throw error as Throwable
      else -> throw UnwrapException(error.toString())
    }
    is Ok<*> -> return value as A
  }
}

public fun <T> List<T>.padTo(size: Int): List<T> {
  return if (this.isEmpty()) {
    emptyList()
  } else if (this.size > size) {
    this
  } else {
    val list = this.toMutableList()
    while (list.size < size) {
      list += this
    }
    list.subList(0, size)
  }
}

public fun <T> Array<T>.padTo(size: Int) = this.asList().padTo(size)
public fun BooleanArray.padTo(size: Int) = this.asList().padTo(size)
public fun LongArray.padTo(size: Int) = this.asList().padTo(size)
public fun IntArray.padTo(size: Int) = this.asList().padTo(size)
public fun DoubleArray.padTo(size: Int) = this.asList().padTo(size)

public fun MutableMap<String, JsonValue>?.deepMerge(map: Map<String, JsonValue>): MutableMap<String, JsonValue> {
  return if (this != null) {
    (this.entries.toList() + map.entries).fold(mutableMapOf()) { map, entry ->
      if (map.containsKey(entry.key)) {
        when (val value = map[entry.key]) {
          is JsonValue.Object -> if (entry.value is JsonValue.Object) {
            map[entry.key] = JsonValue.Object(value.entries.deepMerge((entry.value as JsonValue.Object).entries))
          } else {
            map[entry.key] = entry.value
          }
          is JsonValue.Array -> if (entry.value is JsonValue.Array) {
            map[entry.key] = JsonValue.Array((value.values + (entry.value as JsonValue.Array).values).toMutableList())
          } else {
            map[entry.key] = entry.value
          }
          else -> map[entry.key] = entry.value
        }
      } else {
        map[entry.key] = entry.value
      }
      map
    }
  } else {
    mutableMapOf()
  }
}

sealed class Either<out A, out B> {
  data class A<A>(val value: A) : Either<A, Nothing>()
  data class B<B>(val value: B) : Either<Nothing, B>()

  fun unwrapA(error: String): A {
    return when (this) {
      is Either.A -> this.value
      is Either.B -> throw InvalidEitherOptionException(error)
    }
  }

  fun unwrapB(error: String): B {
    return when (this) {
      is Either.A -> throw InvalidEitherOptionException(error)
      is Either.B -> this.value
    }
  }

  companion object {
    @JvmStatic
    fun <A, B> a(value: A): Either<A, B> {
      return A(value)
    }

    @JvmStatic
    fun <B> b(value: B): Either<Nothing, B> {
      return B(value)
    }
  }
}
