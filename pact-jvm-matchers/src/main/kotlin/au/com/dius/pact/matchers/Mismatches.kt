package au.com.dius.pact.matchers

/**
 * Interface to a mismatch
 */
interface Mismatch {
  fun description(): String
}

/**
 * Interface to a factory class to create a mismatch
 *
 * @param <Mismatch> Type of mismatch to create
 */
interface MismatchFactory<out M: Mismatch> {
  fun create(expected: Any?, actual: Any?, message: String, path: List<String>): M
}

data class HeaderMismatch(val headerKey: String, val expected: String, val actual: String, val mismatch: String? = null) : Mismatch {
  override fun description(): String = if (mismatch != null) {
    "HeaderMismatch - $mismatch"
  } else {
    toString()
  }
}

object HeaderMismatchFactory : MismatchFactory<HeaderMismatch> {
  override fun create(expected: Any?, actual: Any?, message: String, path: List<String>) =
    HeaderMismatch(path.last(), expected.toString(), actual.toString(), message)
}
