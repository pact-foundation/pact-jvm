package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import java.util.regex.Pattern

data class UrlMatcherSupport(val basePath: String, val pathFragments: List<Any>) {
  fun getExampleValue() = basePath + PATH_SEP + pathFragments.joinToString(separator = PATH_SEP) {
    when (it) {
      is RegexMatcher -> it.example!!
      else -> it.toString()
    }
  }

  fun getRegexExpression() = ".*" + pathFragments.joinToString(separator = "\\/") {
    when (it) {
      is RegexMatcher -> it.regex
      else -> Pattern.quote(it.toString())
    }
  } + "$"

  companion object {
    const val PATH_SEP = "/"
  }
}
