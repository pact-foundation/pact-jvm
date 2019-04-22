package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.core.model.OptionalBody;
import au.com.dius.pact.core.model.ProviderState;
import au.com.dius.pact.core.model.Request;
import au.com.dius.pact.core.model.RequestResponseInteraction;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.Response;
import au.com.dius.pact.core.model.generators.Category;
import au.com.dius.pact.core.model.generators.Generators;
import au.com.dius.pact.core.model.generators.ProviderStateGenerator;
import au.com.dius.pact.core.model.matchingrules.MatchingRules;
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;
import au.com.dius.pact.core.model.matchingrules.RuleLogic;
import com.mifmif.common.regex.Generex;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;

public class PactDslResponse {
    private static final String CONTENT_TYPE = "Content-Type";
    static final String DEFAULT_JSON_CONTENT_TYPE_REGEX = "application/json(;\\s?charset=[\\w\\-]+)?";

    private final ConsumerPactBuilder consumerPactBuilder;
    private PactDslRequestWithPath request;
    private final PactDslRequestWithoutPath defaultRequestValues;
    private final PactDslResponse defaultResponseValues;

    private int responseStatus = 200;
    private Map<String, List<String>> responseHeaders = new HashMap<>();
    private OptionalBody responseBody = OptionalBody.missing();
    private MatchingRules responseMatchers = new MatchingRulesImpl();
    private Generators responseGenerators = new Generators();

    public PactDslResponse(ConsumerPactBuilder consumerPactBuilder, PactDslRequestWithPath request,
                           PactDslRequestWithoutPath defaultRequestValues,
                           PactDslResponse defaultResponseValues) {
        this.consumerPactBuilder = consumerPactBuilder;
        this.request = request;
      this.defaultRequestValues = defaultRequestValues;
      this.defaultResponseValues = defaultResponseValues;

      setupDefaultValues();
    }

  private void setupDefaultValues() {
    if (defaultResponseValues != null) {
      responseStatus = defaultResponseValues.responseStatus;
      responseHeaders.putAll(defaultResponseValues.responseHeaders);
      responseBody = defaultResponseValues.responseBody;
      responseMatchers = ((MatchingRulesImpl) defaultResponseValues.responseMatchers).copy();
      responseGenerators = new Generators(defaultResponseValues.responseGenerators.getCategories());
    }
  }

  /**
     * Response status code
     *
     * @param status HTTP status code
     */
    public PactDslResponse status(int status) {
        this.responseStatus = status;
        return this;
    }

    /**
     * Response headers to return
     *
     * Provide the headers you want to validate, other headers will be ignored.
     *
     * @param headers key-value pairs of headers
     */
    public PactDslResponse headers(Map<String, String> headers) {
      for (Map.Entry<String, String> entry: headers.entrySet()) {
        this.responseHeaders.put(entry.getKey(), Collections.singletonList(entry.getValue()));
      }
      return this;
    }

    /**
     * Response body to return
     *
     * @param body Response body in string form
     */
    public PactDslResponse body(String body) {
        this.responseBody = OptionalBody.body(body.getBytes());
        return this;
    }

    /**
     * Response body to return
     *
     * @param body body in string form
     * @param mimeType the Content-Type response header value
     */
    public PactDslResponse body(String body, String mimeType) {
        responseBody = OptionalBody.body(body.getBytes());
        responseHeaders.put(CONTENT_TYPE, Collections.singletonList(mimeType));
        return this;
    }

    /**
     * Response body to return
     *
     * @param body body in string form
     * @param mimeType the Content-Type response header value
     */
    public PactDslResponse body(String body, ContentType mimeType) {
        return body(body, mimeType.toString());
    }

    /**
     * The body of the request
     *
     * @param body Response body in Java Functional Interface Supplier that must return a string
     */
    public PactDslResponse body(Supplier<String> body) {
        responseBody = OptionalBody.body(body.get().getBytes());
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Response body in Java Functional Interface Supplier that must return a string
     * @param mimeType the Content-Type response header value
     */
    public PactDslResponse body(Supplier<String> body, String mimeType) {
        responseBody = OptionalBody.body(body.get().getBytes());
        responseHeaders.put(CONTENT_TYPE, Collections.singletonList(mimeType));
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Response body in Java Functional Interface Supplier that must return a string
     * @param mimeType the Content-Type response header value
     */
    public PactDslResponse body(Supplier<String> body, ContentType mimeType) {
        return body(body, mimeType.toString());
    }


    /**
     * The body of the request with possible single quotes as delimiters
     * and using {@link QuoteUtil} to convert single quotes to double quotes if required.
     *
     * @param body Request body in string form
     */
    public PactDslResponse bodyWithSingleQuotes(String body) {
        if (body != null) {
            body = QuoteUtil.convert(body);
        }
        return body(body);
    }

    /**
     * The body of the request with possible single quotes as delimiters
     * and using {@link QuoteUtil} to convert single quotes to double quotes if required.
     *
     * @param body Request body in string form
     * @param mimeType the Content-Type response header value
     */
    public PactDslResponse bodyWithSingleQuotes(String body, String mimeType) {
        if (body != null) {
            body = QuoteUtil.convert(body);
        }
        return body(body, mimeType);
    }

    /**
     * The body of the request with possible single quotes as delimiters
     * and using {@link QuoteUtil} to convert single quotes to double quotes if required.
     *
     * @param body Request body in string form
     * @param mimeType the Content-Type response header value
     */
    public PactDslResponse bodyWithSingleQuotes(String body, ContentType mimeType) {
        return bodyWithSingleQuotes(body, mimeType.toString());
    }

    /**
     * Response body to return
     *
     * @param body Response body in JSON form
     */
    public PactDslResponse body(JSONObject body) {
        this.responseBody = OptionalBody.body(body.toString().getBytes());
        if (!responseHeaders.containsKey(CONTENT_TYPE)) {
            matchHeader(CONTENT_TYPE, DEFAULT_JSON_CONTENT_TYPE_REGEX, ContentType.APPLICATION_JSON.toString());
        }
        return this;
    }

    /**
     * Response body to return
     *
     * @param body Response body built using the Pact body DSL
     */
    public PactDslResponse body(DslPart body) {
        DslPart parent = body.close();

        if (parent instanceof PactDslJsonRootValue) {
          ((PactDslJsonRootValue)parent).setEncodeJson(true);
        }

        responseMatchers.addCategory(parent.getMatchers());
        responseGenerators.addGenerators(parent.generators);
        if (parent.getBody() != null) {
            responseBody = OptionalBody.body(parent.getBody().toString().getBytes());
        } else {
            responseBody = OptionalBody.nullBody();
        }

        if (!responseHeaders.containsKey(CONTENT_TYPE)) {
            matchHeader(CONTENT_TYPE, DEFAULT_JSON_CONTENT_TYPE_REGEX, ContentType.APPLICATION_JSON.toString());
        }
        return this;
    }

    /**
     * Response body to return
     *
     * @param body Response body as an XML Document
     */
    public PactDslResponse body(Document body) throws TransformerException {
        responseBody = OptionalBody.body(ConsumerPactBuilder.xmlToString(body).getBytes());
        if (!responseHeaders.containsKey(CONTENT_TYPE)) {
            responseHeaders.put(CONTENT_TYPE, Collections.singletonList(ContentType.APPLICATION_XML.toString()));
        }
        return this;
    }

    /**
     * Match a response header. A random example header value will be generated from the provided regular expression.
     *
     * @param header Header to match
     * @param regexp Regular expression to match
     */
    public PactDslResponse matchHeader(String header, String regexp) {
        return matchHeader(header, regexp, new Generex(regexp).random());
    }

    /**
     * Match a response header.
     *
     * @param header        Header to match
     * @param regexp        Regular expression to match
     * @param headerExample Example value to use
     */
    public PactDslResponse matchHeader(String header, String regexp, String headerExample) {
        responseMatchers.addCategory("header").setRule(header, new RegexMatcher(regexp));
        responseHeaders.put(header, Collections.singletonList(headerExample));
        return this;
    }

    private void addInteraction() {
        consumerPactBuilder.getInteractions().add(new RequestResponseInteraction(
          request.description,
          request.state,
          new Request(request.requestMethod, request.path, request.query,
            request.requestHeaders, request.requestBody, request.requestMatchers, request.requestGenerators),
          new Response(responseStatus, responseHeaders, responseBody, responseMatchers, responseGenerators)
        ));
    }

    /**
     * Terminates the DSL and builds a pact to represent the interactions
     */
    public RequestResponsePact toPact() {
        addInteraction();
        return new RequestResponsePact(request.provider, request.consumer, consumerPactBuilder.getInteractions());
    }

    /**
     * Description of the request that is expected to be received
     *
     * @param description request description
     */
    public PactDslRequestWithPath uponReceiving(String description) {
        addInteraction();
        return new PactDslRequestWithPath(consumerPactBuilder, request, description, defaultRequestValues,
          defaultResponseValues);
    }

    /**
     * Adds a provider state to this interaction
     * @param state Description of the state
     */
    public PactDslWithState given(String state) {
        addInteraction();
        return new PactDslWithState(consumerPactBuilder, request.consumer.getName(), request.provider.getName(),
          new ProviderState(state), defaultRequestValues, defaultResponseValues);
    }

    /**
     * Adds a provider state to this interaction
     * @param state Description of the state
     * @param params Data parameters for this state
     */
    public PactDslWithState given(String state, Map<String, Object> params) {
      addInteraction();
      return new PactDslWithState(consumerPactBuilder, request.consumer.getName(), request.provider.getName(),
        new ProviderState(state, params), defaultRequestValues, defaultResponseValues);
    }

  /**
   * Adds a header that will have it's value injected from the provider state
   * @param name Header Name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  public PactDslResponse headerFromProviderState(String name, String expression, String example) {
    responseGenerators.addGenerator(Category.HEADER, name, new ProviderStateGenerator(expression));
    responseHeaders.put(name, Collections.singletonList(example));
    return this;
  }

  /**
   * Match a set cookie header
   * @param cookie Cookie name to match
   * @param regex Regex to match the cookie value with
   * @param example Example value
   */
  public PactDslResponse matchSetCookie(String cookie, String regex, String example) {
    au.com.dius.pact.core.model.matchingrules.Category header = responseMatchers.addCategory("header");
    if (header.numRules("set-cookie") > 0) {
      header.addRule("set-cookie", new RegexMatcher(Pattern.quote(cookie + "=") + regex));
    } else {
      header.setRule("set-cookie", new RegexMatcher(Pattern.quote(cookie + "=") + regex), RuleLogic.OR);
    }
    if (responseHeaders.containsKey("set-cookie")) {
      responseHeaders.get("set-cookie").add(cookie + "=" + example);
    } else {
      responseHeaders.put("set-cookie", newArrayList(cookie + "=" + example));
    }
    return this;
  }
}
