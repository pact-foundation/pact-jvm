package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.generators.Generator
import au.com.dius.pact.model.matchingrules.MatchingRule
import groovy.transform.Canonical

/**
 * Base class for matchers
 */
@Canonical
class Matcher {
  def value
  MatchingRule matcher = null
  Generator generator = null
}
