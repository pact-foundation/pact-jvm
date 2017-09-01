package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.MatchingRule
import au.com.dius.pact.model.matchingrules.RegexMatcher

import java.util.regex.Pattern

/**
 * Match a URL by specifying the base and a series of paths.
 */
class UrlMatcher extends Matcher {

  private static final String PATH_SEP = '/'

  private final String basePath
  private final List pathFragments

  UrlMatcher(String basePath, List pathFragments) {
    this.pathFragments = pathFragments
    this.basePath = basePath
    this.value = basePath + PATH_SEP + pathFragments.collect {
      if (it instanceof RegexpMatcher) {
        it.value
      } else {
        it.toString()
      }
    }.join(PATH_SEP)
  }

  MatchingRule getMatcher() {
    new RegexMatcher('.*' + pathFragments.collect {
      if (it instanceof RegexpMatcher) {
        it.regex
      } else {
        Pattern.quote(it.toString())
      }
    }.join('\\/') + '$')
  }
}
