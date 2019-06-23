package au.com.dius.pact.core.matchers

/**
 * Interface to a factory class to create a mismatch
 *
 * @param <Mismatch> Type of mismatch to create
 */
@Deprecated("Use the mismatch class constructor directly")
interface MismatchFactory<out M : Mismatch> {
  @Deprecated("Use the mismatch class constructor directly")
  fun create(expected: Any?, actual: Any?, message: String, path: List<String>): M
}

sealed class Mismatch {
  open fun description() = this.toString()
}

data class StatusMismatch(val expected: Int, val actual: Int) : Mismatch() {
  override fun description() = "expected status of $expected but was $actual"
}

data class BodyTypeMismatch(val expected: String, val actual: String) : Mismatch() {
  override fun description() = "Expected a response type of '$expected' but the actual type was '$actual'"
}

data class CookieMismatch(val expected: List<String>, val actual: List<String>) : Mismatch()

data class PathMismatch @JvmOverloads constructor (
  val expected: String,
  val actual: String,
  val mismatch: String? = null
) : Mismatch() {
  override fun description() = when (mismatch) {
    null -> super.description()
    else -> "PathMismatch - $mismatch"
  }
}

object PathMismatchFactory : MismatchFactory<PathMismatch> {
  override fun create(expected: Any?, actual: Any?, message: String, path: List<String>) =
    PathMismatch(expected.toString(), actual.toString(), message)
}

data class MethodMismatch(val expected: String, val actual: String) : Mismatch()

data class QueryMismatch(
  val queryParameter: String,
  val expected: String,
  val actual: String,
  val mismatch: String? = null,
  val path: String = "/"
) : Mismatch()

object QueryMismatchFactory : MismatchFactory<QueryMismatch> {
    override fun create(expected: Any?, actual: Any?, message: String, path: List<String>) =
      QueryMismatch(path.last(), expected.toString(), actual.toString(), message)
}

data class HeaderMismatch(
  val headerKey: String,
  val expected: String,
  val actual: String,
  val mismatch: String
) : Mismatch() {
  override fun description() = mismatch

  fun merge(mismatch: HeaderMismatch): HeaderMismatch {
    return if (this.mismatch.isNotEmpty()) {
      copy(mismatch = this.mismatch + ", " + mismatch.mismatch)
    } else {
      copy(mismatch = mismatch.mismatch)
    }
  }
}

object HeaderMismatchFactory : MismatchFactory<HeaderMismatch> {
  override fun create(expected: Any?, actual: Any?, message: String, path: List<String>) =
    HeaderMismatch(path.last(), expected.toString(), actual.toString(), message)
}

data class BodyMismatch @JvmOverloads constructor(
  val expected: Any?,
  val actual: Any?,
  val mismatch: String,
  val path: String = "/",
  val diff: String? = null
) : Mismatch() {
  override fun description() = mismatch
}

object BodyMismatchFactory : MismatchFactory<BodyMismatch> {
  override fun create(expected: Any?, actual: Any?, message: String, path: List<String>) =
    BodyMismatch(expected, actual, message, path.joinToString("."))
}

data class MetadataMismatch(val key: String, val expected: Any?, val actual: Any?, val mismatch: String) : Mismatch() {
  override fun description() = mismatch

  fun merge(mismatch: MetadataMismatch) = copy(mismatch = this.mismatch + ", " + mismatch.mismatch)
}

object MetadataMismatchFactory : MismatchFactory<MetadataMismatch> {
  override fun create(expected: Any?, actual: Any?, message: String, path: List<String>) =
    MetadataMismatch(path.last(), expected, actual, message)
}
