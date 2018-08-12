package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.core.model.OptionalBody;
import au.com.dius.pact.core.model.generators.Category;
import au.com.dius.pact.core.model.generators.ProviderStateGenerator;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;
import au.com.dius.pact.core.model.PactReader;
import com.mifmif.common.regex.Generex;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static au.com.dius.pact.consumer.ConsumerPactBuilder.xmlToString;

public class PactDslRequestWithoutPath extends PactDslRequestBase {

  private final ConsumerPactBuilder consumerPactBuilder;
  private PactDslWithState pactDslWithState;
  private String description;
  private String consumerName;
  private String providerName;
  private final PactDslResponse defaultResponseValues;

  public PactDslRequestWithoutPath(ConsumerPactBuilder consumerPactBuilder,
                                   PactDslWithState pactDslWithState,
                                   String description,
                                   PactDslRequestWithoutPath defaultRequestValues,
                                   PactDslResponse defaultResponseValues) {
    super(defaultRequestValues);

    this.consumerPactBuilder = consumerPactBuilder;
    this.pactDslWithState = pactDslWithState;
    this.description = description;
    this.consumerName = pactDslWithState.consumerName;
    this.providerName = pactDslWithState.providerName;
    this.defaultResponseValues = defaultResponseValues;

    setupDefaultValues();
  }

  /**
     * The HTTP method for the request
     *
     * @param method Valid HTTP method
     */
    public PactDslRequestWithoutPath method(String method) {
        requestMethod = method;
        return this;
    }

    /**
     * Headers to be included in the request
     *
     * @param headers Key-value pairs
     */
    public PactDslRequestWithoutPath headers(Map<String, String> headers) {
        requestHeaders = new HashMap<String, String>(headers);
        return this;
    }

    /**
     * Headers to be included in the request
     *
     * @param firstHeaderName      The name of the first header
     * @param firstHeaderValue     The value of the first header
     * @param headerNameValuePairs Additional headers in name-value pairs.
     */
    public PactDslRequestWithoutPath headers(String firstHeaderName, String firstHeaderValue, String... headerNameValuePairs) {
        if (headerNameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Pair key value should be provided, but there is one key without value.");
        }
        requestHeaders.put(firstHeaderName, firstHeaderValue);

        for (int i = 0; i < headerNameValuePairs.length; i+=2) {
            requestHeaders.put(headerNameValuePairs[i], headerNameValuePairs[i+1]);
        }

        return this;
    }

    /**
     * The query string for the request
     *
     * @param query query string
     */
    public PactDslRequestWithoutPath query(String query) {
        this.query = PactReader.queryStringToMap(query, false);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithoutPath body(String body) {
        requestBody = OptionalBody.body(body);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithoutPath body(String body, String mimeType) {
        requestBody = OptionalBody.body(body);
        requestHeaders.put(CONTENT_TYPE, mimeType);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithoutPath body(String body, ContentType mimeType) {
        return body(body, mimeType.toString());
    }


    /**
     * The body of the request
     *
     * @param body Request body in Java Functional Interface Supplier that must return a string
     */
    public PactDslRequestWithoutPath body(Supplier<String> body) {
        requestBody = OptionalBody.body(body.get());
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in Java Functional Interface Supplier that must return a string
     */
    public PactDslRequestWithoutPath body(Supplier<String> body, String mimeType) {
        requestBody = OptionalBody.body(body.get());
        requestHeaders.put(CONTENT_TYPE, mimeType);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in Java Functional Interface Supplier that must return a string
     */
    public PactDslRequestWithoutPath body(Supplier<String> body, ContentType mimeType) {
        return body(body, mimeType.toString());
    }

    /**
     * The body of the request with possible single quotes as delimiters
     * and using {@link QuoteUtil} to convert single quotes to double quotes if required.
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithoutPath bodyWithSingleQuotes(String body) {
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
     */
    public PactDslRequestWithoutPath bodyWithSingleQuotes(String body, String mimeType) {
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
     */
    public PactDslRequestWithoutPath bodyWithSingleQuotes(String body, ContentType mimeType) {
        if (body != null) {
            body = QuoteUtil.convert(body);
        }
        return body(body, mimeType);
    }

    /**
     * The body of the request
     *
     * @param body Request body in JSON form
     */
    public PactDslRequestWithoutPath body(JSONObject body) {
        requestBody = OptionalBody.body(body.toString());
        if (!requestHeaders.containsKey(CONTENT_TYPE)) {
            requestHeaders.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        }
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Built using the Pact body DSL
     */
    public PactDslRequestWithoutPath body(DslPart body) {
        DslPart parent = body.close();
        requestMatchers.addCategory(parent.matchers);
        requestGenerators.addGenerators(parent.generators);
        requestBody = OptionalBody.body(parent.toString());
        if (!requestHeaders.containsKey(CONTENT_TYPE)) {
            requestHeaders.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        }
        return this;
    }

    /**
     * The body of the request
     *
     * @param body XML Document
     */
    public PactDslRequestWithoutPath body(Document body) throws TransformerException {
        requestBody = OptionalBody.body(xmlToString(body));
        if (!requestHeaders.containsKey(CONTENT_TYPE)) {
            requestHeaders.put(CONTENT_TYPE, ContentType.APPLICATION_XML.toString());
        }
        return this;
    }

    /**
     * The path of the request
     *
     * @param path string path
     */
    public PactDslRequestWithPath path(String path) {
        return new PactDslRequestWithPath(consumerPactBuilder, consumerName, providerName, pactDslWithState.state,
          description, path, requestMethod, requestHeaders, query, requestBody, requestMatchers, requestGenerators,
          defaultRequestValues, defaultResponseValues);
    }

    /**
     * The path of the request. This will generate a random path to use when generating requests
     *
     * @param pathRegex string path regular expression to match with
     */
    public PactDslRequestWithPath matchPath(String pathRegex) {
        return matchPath(pathRegex, new Generex(pathRegex).random());
    }

    /**
     * The path of the request
     *
     * @param path      string path to use when generating requests
     * @param pathRegex regular expression to use to match paths
     */
    public PactDslRequestWithPath matchPath(String pathRegex, String path) {
        requestMatchers.addCategory("path").addRule(new RegexMatcher(pathRegex));
        return new PactDslRequestWithPath(consumerPactBuilder, consumerName, providerName, pactDslWithState.state,
          description, path, requestMethod, requestHeaders, query, requestBody, requestMatchers, requestGenerators,
          defaultRequestValues, defaultResponseValues);
    }

    /**
     * Sets up a file upload request. This will add the correct content type header to the request
     * @param partName This is the name of the part in the multipart body.
     * @param fileName This is the name of the file that was uploaded
     * @param fileContentType This is the content type of the uploaded file
     * @param data This is the actual file contents
     */
    public PactDslRequestWithoutPath withFileUpload(String partName, String fileName, String fileContentType, byte[] data)
      throws IOException {
      setupFileUpload(partName, fileName, fileContentType, data);
      return this;
    }

  /**
   * Adds a header that will have it's value injected from the provider state
   * @param name Header Name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  public PactDslRequestWithoutPath headerFromProviderState(String name, String expression, String example) {
    requestGenerators.addGenerator(Category.HEADER, name, new ProviderStateGenerator(expression));
    requestHeaders.put(name, example);
    return this;
  }

  /**
   * Adds a query parameter that will have it's value injected from the provider state
   * @param name Name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  public PactDslRequestWithoutPath queryParameterFromProviderState(String name, String expression, String example) {
    requestGenerators.addGenerator(Category.QUERY, name, new ProviderStateGenerator(expression));
    query.put(name, Collections.singletonList(example));
    return this;
  }

  /**
   * Sets the path to have it's value injected from the provider state
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  public PactDslRequestWithPath pathFromProviderState(String expression, String example) {
    requestGenerators.addGenerator(Category.PATH, new ProviderStateGenerator(expression));
    return new PactDslRequestWithPath(consumerPactBuilder, consumerName, providerName, pactDslWithState.state,
      description, example, requestMethod, requestHeaders, query, requestBody, requestMatchers, requestGenerators,
      defaultRequestValues, defaultResponseValues);
  }

}
