package au.com.dius.pact.matchers

/**
 * Interface to a factory class to create a mismatch
 *
 * @param <Mismatch> Type of mismatch to create
 */
interface MismatchFactory<Mismatch> {
  fun create(expected: Any?, actual: Any?, message: String, path: List<String>): Mismatch
}
