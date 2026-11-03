package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.ContentType
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

object BuilderUtils {
  /**
   * Loads the file given by the file path and returns the contents. Relative paths will be resolved against the
   * current working directory.
   */
  @JvmStatic
  fun textFile(filePath: String): String {
    var path = Paths.get(filePath)
    if (!path.exists()) {
      val cwd = Path.of("").toAbsolutePath()
      path = cwd.resolve(filePath).toAbsolutePath()
    }
    return path.toFile().bufferedReader().readText()
  }

  /**
   * Resolves the given file path. Relative paths will be resolved against the current working directory.
   */
  @JvmStatic
  fun filePath(filePath: String): String {
    var path = Paths.get(filePath).toAbsolutePath()
    if (!path.exists()) {
      val cwd = Path.of("").toAbsolutePath()
      path = cwd.resolve(filePath).toAbsolutePath()
    }
    return path.normalize().toString()
  }

  /**
   * The charset to encode a body of the given content type with. JSON without a charset parameter is UTF-8, other
   * types without one use the platform default charset.
   */
  @JvmStatic
  fun charset(contentType: String): Charset = ContentType(contentType).asCharset()
}
