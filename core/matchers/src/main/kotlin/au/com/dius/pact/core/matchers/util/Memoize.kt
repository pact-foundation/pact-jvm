package au.com.dius.pact.core.matchers.util

private class MemoizeFixed1<out R>(val function: (Int) -> R, size: Int) : (Int) -> R {
  private val cache = MutableList<R?>(size) { null }
  override fun invoke(param: Int): R {
    if (cache[param] == null) {
      cache[param] = function(param)
    }
    return cache[param]!!
  }
}

/**
 * Memoize function using a fixed-sized cache.
 *
 * @param size Fixed size of the cache, allowing for parameter values between [0, size)
 */
fun <R> ((Int) -> R).memoizeFixed(size: Int): (Int) -> R = MemoizeFixed1(this, size)

private class MemoizeFixed2<out R>(val function: (Int, Int) -> R, size1: Int, size2: Int) : (Int, Int) -> R {
  private val cache = MutableList(size1) { MutableList<R?>(size2) { null } }
  override fun invoke(param1: Int, param2: Int): R {
    if (cache[param1][param2] == null) {
      cache[param1][param2] = function(param1, param2)
    }
    return cache[param1][param2]!!
  }
}

/**
 * Memoize function using a fixed-sized cache.
 *
 * @param size1 Fixed size of the cache for the first parameter, allowing for values between [0, size1)
 * @param size2 Fixed size of the cache for the second parameter, allowing for values between [0, size2)
 */
fun <R> ((Int, Int) -> R).memoizeFixed(size1: Int, size2: Int): (Int, Int) -> R =
  MemoizeFixed2(this, size1, size2)
