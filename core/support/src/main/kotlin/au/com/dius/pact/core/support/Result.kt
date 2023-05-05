package au.com.dius.pact.core.support

sealed class Result<out V, out E> {
  data class Ok<out V>(val value: V): Result<V, Nothing>()
  data class Err<out E>(val error: E): Result<Nothing, E>()

  fun unwrap(): V {
    when (this) {
      is Ok -> return value
      is Err -> if (error is Throwable) {
        throw error
      } else {
        throw UnwrapException("Tried to unwrap an Err value $error")
      }
    }
  }

  fun expect(function: () -> String): V {
    when (this) {
      is Ok -> return value
      is Err -> throw UnwrapException(function())
    }
  }

  fun get(): V? {
    return when (this) {
      is Err -> null
      is Ok -> value
    }
  }

  fun errorValue(): E? {
    return when (this) {
      is Err -> error
      is Ok -> null
    }
  }
}

fun <V1, V2, E> Result<V1, E>.mapOk(transform: (V1) -> V2): Result<V2, E> {
  return when (this) {
    is Result.Ok -> Result.Ok(transform(value))
    is Result.Err -> this
  }
}

fun <V, E1, E2> Result<V, E1>.mapError(transform: (E1) -> E2): Result<V, E2> {
  return when (this) {
    is Result.Ok -> this
    is Result.Err -> Result.Err(transform(error))
  }
}

fun <V1, V2, E1, E2> Result<V1, E1>.mapEither(successFn: (V1) -> V2, errorFn: (E1) -> E2): Result<V2, E2> {
  return when (this) {
    is Result.Err -> Result.Err(errorFn(error))
    is Result.Ok -> Result.Ok(successFn(value))
  }
}

fun <V, E> Result<V, E>.getOr(default: V): V {
  return when (this) {
    is Result.Err -> default
    is Result.Ok -> value
  }
}

fun <V, E> Result<V, E>.getOrElse(function: (E) -> V): V {
  return when (this) {
    is Result.Err -> function(error)
    is Result.Ok -> value
  }
}

fun <V, E> Result<V, E>.orElse(function: (E) -> Result<V, E>): Result<V, E> {
  return when (this) {
    is Result.Err -> function(error)
    is Result.Ok -> this
  }
}

fun <V, E> Result<V, E>.or(default: Result<V, E>): Result<V, E> {
  return when (this) {
    is Result.Err -> default
    is Result.Ok -> this
  }
}

class UnwrapException(message: String): RuntimeException(message)
