package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.MatchingRule

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
    value = basePath + PATH_SEP + pathFragments.join(PATH_SEP)
  }

  MatchingRule getMatcher() {
    new au.com.dius.pact.model.matchingrules.IncludeMatcher(pathFragments.join(PATH_SEP))
  }
}
