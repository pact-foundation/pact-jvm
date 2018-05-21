package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.model.OptionalBody;
import au.com.dius.pact.model.generators.Generators;
import au.com.dius.pact.model.matchingrules.MatchingRules;
import au.com.dius.pact.model.matchingrules.MatchingRulesImpl;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PactDslRequestBase {
  protected final PactDslRequestWithoutPath defaultRequestValues;
  protected String requestMethod;
  protected Map<String, String> requestHeaders = new HashMap<>();
  protected Map<String, List<String>> query = new HashMap<>();
  protected OptionalBody requestBody = OptionalBody.missing();
  protected MatchingRules requestMatchers = new MatchingRulesImpl();
  protected Generators requestGenerators = new Generators();

  public PactDslRequestBase(PactDslRequestWithoutPath defaultRequestValues) {
    this.defaultRequestValues = defaultRequestValues;
  }

  protected void setupDefaultValues() {
    if (defaultRequestValues != null) {
      if (StringUtils.isNotEmpty(defaultRequestValues.requestMethod)) {
        requestMethod = defaultRequestValues.requestMethod;
      }
      requestHeaders.putAll(defaultRequestValues.requestHeaders);
      query.putAll(defaultRequestValues.query);
      requestBody = defaultRequestValues.requestBody;
      requestMatchers = ((MatchingRulesImpl) defaultRequestValues.requestMatchers).copy();
      requestGenerators = new Generators(defaultRequestValues.requestGenerators.getCategories());
    }
  }
}
