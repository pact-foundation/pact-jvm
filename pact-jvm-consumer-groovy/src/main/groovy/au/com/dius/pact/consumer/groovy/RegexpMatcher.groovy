package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.generators.Generator
import au.com.dius.pact.model.generators.RegexGenerator
import au.com.dius.pact.model.matchingrules.MatchingRule
import au.com.dius.pact.model.matchingrules.RegexMatcher
import com.mifmif.common.regex.Generex

/**
 * Regular Expression Matcher
 */
class RegexpMatcher extends Matcher {

  String regex

  MatchingRule getMatcher() {
    new RegexMatcher(regex)
  }

  Generator getGenerator() {
    value == null ? new RegexGenerator(regex) : null
  }

  def getValue() {
    super.@value ?: new Generex(regex).random()
  }

}
