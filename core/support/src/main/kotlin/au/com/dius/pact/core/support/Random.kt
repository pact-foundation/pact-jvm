package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.regex.RegexParser
import au.com.dius.pact.core.support.regex.RegexStringGenerator

/**
 * Support for the generation of random values
 */
object Random {
  /**
   * Generate a random string from a regular expression
   */
  @JvmStatic
  fun generateRandomString(regex: String): String {
    val cleaned = if (regex.endsWith('$') && !regex.endsWith("\\$")) {
      regex.trimStart('^').trimEnd('$')
    } else {
      regex.trimStart('^')
    }
    return RegexStringGenerator().generate(RegexParser(cleaned).parse())
  }
}
