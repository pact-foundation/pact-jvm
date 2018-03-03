package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.Headers;
import au.com.dius.pact.model.OptionalBody;
import au.com.dius.pact.model.generators.Generators;
import au.com.dius.pact.model.matchingrules.MatchingRules;
import au.com.dius.pact.model.matchingrules.MatchingRulesImpl;
import au.com.dius.pact.model.matchingrules.RegexMatcher;
import au.com.dius.pact.model.PactReader;
import com.mifmif.common.regex.Generex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONObject;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static au.com.dius.pact.consumer.ConsumerPactBuilder.xmlToString;

public class PactDslRequestWithoutPath {
  private static final String CONTENT_TYPE = "Content-Type";

    private final ConsumerPactBuilder consumerPactBuilder;
    private PactDslWithState pactDslWithState;
    private String description;
    private String requestMethod;
    private Map<String, String> requestHeaders = new HashMap<>();
    private Map<String, List<String>> query = new HashMap<>();
    private OptionalBody requestBody = OptionalBody.missing();
    private MatchingRules requestMatchers = new MatchingRulesImpl();
    private Generators requestGenerators = new Generators();
    private String consumerName;
    private String providerName;
  private final PactDslRequestWithoutPath defaultRequestValues;
  private final PactDslResponse defaultResponseValues;

  public PactDslRequestWithoutPath(ConsumerPactBuilder consumerPactBuilder,
                                     PactDslWithState pactDslWithState,
                                     String description,
                                     PactDslRequestWithoutPath defaultRequestValues,
                                     PactDslResponse defaultResponseValues) {
        this.consumerPactBuilder = consumerPactBuilder;
        this.pactDslWithState = pactDslWithState;
        this.description = description;
        this.consumerName = pactDslWithState.consumerName;
        this.providerName = pactDslWithState.providerName;
    this.defaultRequestValues = defaultRequestValues;
    this.defaultResponseValues = defaultResponseValues;

    setupDefaultValues();
  }

  private void setupDefaultValues() {
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
        HttpEntity multipart = MultipartEntityBuilder.create()
          .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
          .addBinaryBody(partName, data, ContentType.create(fileContentType), fileName)
          .build();
        OutputStream os = new ByteArrayOutputStream();
        multipart.writeTo(os);

        requestBody = OptionalBody.body(os.toString());
        requestMatchers.addCategory("header").addRule(CONTENT_TYPE, new RegexMatcher(Headers.MULTIPART_HEADER_REGEX,
          multipart.getContentType().getValue()));
        requestHeaders.put(CONTENT_TYPE, multipart.getContentType().getValue());

        return this;
    }
}
