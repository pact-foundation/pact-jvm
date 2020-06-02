package au.com.dius.pact.core.matchers.util

import java.math.BigInteger
import java.math.BigInteger.ONE

/**
 * Represents a combination of indices for a collection using only a single
 * [BigInteger].
 *
 * @param comboId identifier for a unique combination of actual indexes
 *           where 0 is {}, 1 is {0}, 2 is {1}, 3 is {0, 1}, etc...
 */
class IndicesCombination private constructor(val comboId: BigInteger) {

  companion object {
    @JvmStatic
    fun of(c: Collection<Any>) = of(c.size)

    @JvmStatic
    fun of(size: Int) = IndicesCombination(powOf2(size) - ONE)

    /** @return 2^exp */
    private fun powOf2(exp: Int) = ONE.shiftLeft(exp)
  }

  /**
   * Immutable operation to create new combination with the specified index removed.
   *
   * @param index to remove
   * @return new combination with the removed index
   */
  operator fun minus(index: Int) = IndicesCombination(comboId.clearBit(index))

  /** @return sequence of actual indices in combination */
  fun indices() = sequence {
    var index = 0
    var factor = ONE

    while (factor <= comboId) {
      val left = factor shl 1
      if ((comboId % left) / factor == ONE) {
        yield(index)
      }
      factor = left
      index++
    }
  }
}
