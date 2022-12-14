package au.com.dius.pact.consumer.dsl

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

object BuilderUtils {
  @JvmStatic
  fun textFile(filePath: String): String {
    var path = Paths.get(filePath)
    if (!path.exists()) {
      val cwd = Path.of("").toAbsolutePath()
      path = cwd.resolve(filePath).toAbsolutePath()
    }
    return path.toFile().bufferedReader().readText()
  }

  @JvmStatic
  fun filePath(filePath: String): String {
    var path = Paths.get(filePath).toAbsolutePath()
    if (!path.exists()) {
      val cwd = Path.of("").toAbsolutePath()
      path = cwd.resolve(filePath).toAbsolutePath()
    }
    return path.normalize().toString()
  }
}
