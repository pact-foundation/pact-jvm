package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.dsl.UrlMatcherSupport
import au.com.dius.pact.model.matchingrules.MatchingRule
import au.com.dius.pact.model.matchingrules.RegexMatcher

/**
 * Match a URL by specifying the base and a series of paths.
 */
class UrlMatcher extends Matcher {

  private final String basePath
  private final List pathFragments
  private final UrlMatcherSupport urlMatcherSupport

  UrlMatcher(String basePath, List pathFragments) {
    this.pathFragments = pathFragments
    this.basePath = basePath
    this.urlMatcherSupport = new UrlMatcherSupport(basePath, pathFragments)
    this.value = urlMatcherSupport.exampleValue
  }

  MatchingRule getMatcher() {
    new RegexMatcher(urlMatcherSupport.regexExpression)
  }
}
