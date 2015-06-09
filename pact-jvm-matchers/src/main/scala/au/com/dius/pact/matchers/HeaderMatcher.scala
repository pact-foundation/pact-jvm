package au.com.dius.pact.matchers

import au.com.dius.pact.model.{HeaderMismatchFactory, HeaderMismatch}

object HeaderMatcher {
  def compareHeader(headerKey: String, expected: String, actual: String, matchers: Option[Map[String, Map[String, String]]]) = {
    def stripWhiteSpaceAfterCommas(in: String): String = in.replaceAll(",[ ]*", ",")

    if (Matchers.matcherDefined(Seq("$", "headers", headerKey), matchers)) {
      val mismatch = Matchers.domatch[HeaderMismatch](matchers, Seq("$", "headers", headerKey), expected,
        actual, HeaderMismatchFactory)
      mismatch.headOption
    } else if (stripWhiteSpaceAfterCommas(expected) == stripWhiteSpaceAfterCommas(actual)) None
    else Some(HeaderMismatch(headerKey, expected, actual,
      Some(s"Expected header '$headerKey' to have value '$expected' but was '$actual'")))
  }
}
