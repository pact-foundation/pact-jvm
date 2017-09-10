/**
 * This file inlined from https://github.com/kittinunf/Result
 */
package au.com.dius.pact.provider.broker.com.github.kittinunf.result

inline fun <reified X> Result<*, *>.getAs() = when (this) {
    is Result.Success -> value as? X
    is Result.Failure -> error as? X
}

fun <V : Any> Result<V, *>.success(f: (V) -> Unit) = fold(f, {})

fun <E : Exception> Result<*, E>.failure(f: (E) -> Unit) = fold({}, f)

infix fun <V : Any, E : Exception> Result<V, E>.or(fallback: V) = when (this) {
    is Result.Success -> this
    else -> Result.Success<V, E>(fallback)
}

infix fun <V : Any, E : Exception> Result<V, E>.getOrElse(fallback: V) = when (this) {
    is Result.Success -> value
    else -> fallback
}

fun <V : Any, U : Any, E : Exception> Result<V, E>.map(transform: (V) -> U): Result<U, E> = when (this) {
    is Result.Success -> Result.Success<U, E>(transform(value))
    is Result.Failure -> Result.Failure<U, E>(error)
}

fun <V : Any, U : Any, E : Exception> Result<V, E>.flatMap(transform: (V) -> Result<U, E>): Result<U, E> = when (this) {
    is Result.Success -> transform(value)
    is Result.Failure -> Result.Failure<U, E>(error)
}

fun <V : Any, E : Exception, E2 : Exception> Result<V, E>.mapError(transform: (E) -> E2) = when (this) {
    is Result.Success -> Result.Success<V, E2>(value)
    is Result.Failure -> Result.Failure<V, E2>(transform(error))
}

fun <V : Any, E : Exception, E2 : Exception> Result<V, E>.flatMapError(transform: (E) -> Result<V, E2>) = when (this) {
    is Result.Success -> Result.Success<V, E2>(value)
    is Result.Failure -> transform(error)
}

fun <V : Any> Result<V, *>.any(predicate: (V) -> Boolean): Boolean = when (this) {
    is Result.Success -> predicate(value)
    is Result.Failure -> false
}

fun <V : Any, U: Any> Result<V, *>.fanout(other: () -> Result<U, *>): Result<Pair<V, U>, *> =
    flatMap { outer -> other().map { outer to it } }

sealed class Result<out V : Any, out E : Exception> {

    abstract operator fun component1(): V?
    abstract operator fun component2(): E?

    inline fun <X> fold(success: (V) -> X, failure: (E) -> X): X {
      return when (this) {
        is Success -> success(this.value)
        is Failure -> failure(this.error)
      }
    }

    abstract fun get(): V

    class Success<out V : Any, out E : Exception>(val value: V) : Result<V, E>() {
        override fun component1(): V? = value
        override fun component2(): E? = null

        override fun get(): V = value

        override fun toString() = "[Success: $value]"

        override fun hashCode(): Int = value.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Success<*, *> && value == other.value
        }
    }

    class Failure<out V : Any, out E : Exception>(val error: E) : Result<V, E>() {
        override fun component1(): V? = null
        override fun component2(): E? = error

        override fun get(): V = throw error

        fun getException(): E = error

        override fun toString() = "[Failure: $error]"

        override fun hashCode(): Int = error.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Failure<*, *> && error == other.error
        }
    }

    companion object {
        // Factory methods
        fun <E : Exception> error(ex: E) = Failure<Nothing, E>(ex)

        fun <V : Any> of(value: V?, fail: (() -> Exception) = { Exception() }): Result<V, Exception> {
            return value?.let { Success<V, Nothing>(it) } ?: error(fail())
        }

        fun <V : Any> of(f: () -> V): Result<V, Exception> = try {
          Success(f())
        } catch (ex: Exception) {
          Failure(ex)
        }
    }

}
