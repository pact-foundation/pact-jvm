package au.com.dius.pact.matchers

object HeaderMatcher {
  def compareHeader(headerKey: String, expected: String, actual: String): Boolean = {
    def stripWhiteSpaceAfterCommas(in: String): String = in.replaceAll(",[ ]*", ",")
    stripWhiteSpaceAfterCommas(expected) == stripWhiteSpaceAfterCommas(actual)
  }
}
