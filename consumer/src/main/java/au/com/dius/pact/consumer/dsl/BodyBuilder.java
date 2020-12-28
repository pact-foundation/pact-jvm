package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.core.model.ContentType;
import au.com.dius.pact.core.model.generators.Generators;
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory;

/**
 * Interface to a builder that constructs a body, including matchers and generators
 */
public interface BodyBuilder {
  /**
   * Returns the matchers for the body
   */
  MatchingRuleCategory getMatchers();

  /**
   * Returns the generators for the body
   */
  Generators getGenerators();

  /**
   * Returns the content type for the body
   */
  ContentType getContentType();

  /**
   * Constructs the body returning the contents as a byte array
   */
  byte[] buildBody();
}
