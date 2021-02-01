package au.com.dius.pact.core.model.annotations

import java.lang.annotation.Inherited

/**
 * Used to point Pact runner to the directory where the pact files are stored
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Inherited
annotation class PactDirectory(
  /**
   * @return path to directory of project resource folder with pacts
   */
  val value: String
)
