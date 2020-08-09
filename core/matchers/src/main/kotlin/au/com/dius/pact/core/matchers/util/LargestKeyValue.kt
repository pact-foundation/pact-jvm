package au.com.dius.pact.core.matchers.util

/**
 * Tracks the key/value pair with the largest key.
 */
class LargestKeyValue<K, V> where K : Comparable<K> {
  var key: K? = null
  var value: V? = null

  /**
   * Use key and value if key is larger than current key.
   */
  fun useIfLarger(key: K, value: V) {
    if (this.key == null || key > this.key!!) {
      this.key = key
      this.value = value
    }
  }
}
