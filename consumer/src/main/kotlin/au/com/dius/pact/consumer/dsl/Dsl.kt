package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.validPathCharacter
import org.apache.commons.lang3.StringUtils

object Dsl {
  /**
   * Creates a builder to define the matchers on an array of JSON primitives
   */
  @JvmStatic
  fun arrayOfPrimitives() = ArrayOfPrimitivesBuilder()

  /**
   * Returns a safe matcher key for the attribute name
   */
  @JvmStatic
  fun matcherKey(name: String, rootPath: String): String {
    return if (name.any { !validPathCharacter(it) }) {
      "${StringUtils.stripEnd(rootPath, ".")}['$name']"
    } else {
      rootPath + name
    }
  }
}
