package au.com.dius.pact.core.support

import com.mifmif.common.regex.Generex

/**
 * Support for the generator of random values
 */
object Random {
  /**
   * Generate a random string from a regular expression
   */
  @JvmStatic
  fun generateRandomString(regex: String): String {
    return if (regex.endsWith('$') && !regex.endsWith("\\$")) {
      Generex(regex.trimStart('^').trimEnd('$')).random()
    } else {
      Generex(regex.trimStart('^')).random()
    }
  }
}
