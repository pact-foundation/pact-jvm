package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.Headers;
import au.com.dius.pact.model.Consumer;
import au.com.dius.pact.model.OptionalBody;
import au.com.dius.pact.model.PactReader;
import au.com.dius.pact.model.Provider;
import au.com.dius.pact.model.ProviderState;
import au.com.dius.pact.model.generators.Generators;
import au.com.dius.pact.model.matchingrules.MatchingRules;
import au.com.dius.pact.model.matchingrules.MatchingRulesImpl;
import au.com.dius.pact.model.matchingrules.RegexMatcher;
import com.mifmif.common.regex.Generex;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PactDslRequestWithPath extends PactDslRequestBase {
  private static final String CONTENT_TYPE = "Content-Type";

  private final ConsumerPactBuilder consumerPactBuilder;

  Consumer consumer;
  Provider provider;

  List<ProviderState> state;
  String description;
  String path = "/";
  private final PactDslResponse defaultResponseValues;

  PactDslRequestWithPath(ConsumerPactBuilder consumerPactBuilder,
                        String consumerName,
                        String providerName,
                        List<ProviderState> state,
                        String description,
                        String path,
                        String requestMethod,
                        Map<String, String> requestHeaders,
                        Map<String, List<String>> query,
                        OptionalBody requestBody,
                        MatchingRules requestMatchers,
                        Generators requestGenerators,
                        PactDslRequestWithoutPath defaultRequestValues,
                        PactDslResponse defaultResponseValues) {
    super(defaultRequestValues);

    this.consumerPactBuilder = consumerPactBuilder;
    this.requestMatchers = requestMatchers;
    this.consumer = new Consumer(consumerName);
    this.provider = new Provider(providerName);

    this.state = state;

    this.description = description;
    this.path = path;
    this.requestMethod = requestMethod;
    this.requestHeaders = requestHeaders;
    this.query = query;
    this.requestBody = requestBody;
    this.requestMatchers = requestMatchers;
    this.requestGenerators = requestGenerators;
    this.defaultResponseValues = defaultResponseValues;

    setupDefaultValues();
  }

  PactDslRequestWithPath(ConsumerPactBuilder consumerPactBuilder,
                         PactDslRequestWithPath existing,
                         String description,
                         PactDslRequestWithoutPath defaultRequestValues,
                         PactDslResponse defaultResponseValues) {
    super(defaultRequestValues);

    this.requestMethod = "GET";

    this.consumerPactBuilder = consumerPactBuilder;
    this.consumer = existing.consumer;
    this.provider = existing.provider;
    this.state = existing.state;
    this.description = description;
    this.defaultResponseValues = defaultResponseValues;

    setupDefaultValues();
  }

    /**
     * The HTTP method for the request
     *
     * @param method Valid HTTP method
     */
    public PactDslRequestWithPath method(String method) {
        requestMethod = method;
        return this;
    }

    /**
     * Headers to be included in the request
     *
     * @param firstHeaderName      The name of the first header
     * @param firstHeaderValue     The value of the first header
     * @param headerNameValuePairs Additional headers in name-value pairs.
     */
    public PactDslRequestWithPath headers(String firstHeaderName, String firstHeaderValue, String... headerNameValuePairs) {
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
     * Headers to be included in the request
     *
     * @param headers Key-value pairs
     */
    public PactDslRequestWithPath headers(Map<String, String> headers) {
        requestHeaders.putAll(headers);
        return this;
    }

    /**
     * The query string for the request
     *
     * @param query query string
     */
    public PactDslRequestWithPath query(String query) {
        this.query = PactReader.queryStringToMap(query, false);
        return this;
    }

    /**
     * The encoded query string for the request
     *
     * @param query query string
     */
    public PactDslRequestWithPath encodedQuery(String query) {
        this.query = PactReader.queryStringToMap(query, true);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithPath body(String body) {
        requestBody = OptionalBody.body(body);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithPath body(String body, String mimeType) {
        requestBody = OptionalBody.body(body);
        requestHeaders.put(CONTENT_TYPE, mimeType);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithPath body(String body, ContentType mimeType) {
        return body(body, mimeType.toString());
    }

    /**
     * The body of the request
     *
     * @param body Request body in Java Functional Interface Supplier that must return a string
     */
    public PactDslRequestWithPath body(Supplier<String> body) {
        requestBody = OptionalBody.body(body.get());
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in Java Functional Interface Supplier that must return a string
     */
    public PactDslRequestWithPath body(Supplier<String> body, String mimeType) {
        requestBody = OptionalBody.body(body.get());
        requestHeaders.put(CONTENT_TYPE, mimeType);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in Java Functional Interface Supplier that must return a string
     */
    public PactDslRequestWithPath body(Supplier<String> body, ContentType mimeType) {
        return body(body, mimeType.toString());
    }

    /**
     * The body of the request with possible single quotes as delimiters
     * and using {@link QuoteUtil} to convert single quotes to double quotes if required.
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithPath bodyWithSingleQuotes(String body) {
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
    public PactDslRequestWithPath bodyWithSingleQuotes(String body, String mimeType) {
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
    public PactDslRequestWithPath bodyWithSingleQuotes(String body, ContentType mimeType) {
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
    public PactDslRequestWithPath body(JSONObject body) {
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
    public PactDslRequestWithPath body(DslPart body) {
        DslPart parent = body.close();
        requestMatchers.addCategory(parent.getMatchers());
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
    public PactDslRequestWithPath body(Document body) throws TransformerException {
        requestBody = OptionalBody.body(ConsumerPactBuilder.xmlToString(body));
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
        this.path = path;
        return this;
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
        this.path = path;
        return this;
    }

    /**
     * Match a request header. A random example header value will be generated from the provided regular expression.
     *
     * @param header Header to match
     * @param regex  Regular expression to match
     */
    public PactDslRequestWithPath matchHeader(String header, String regex) {
        return matchHeader(header, regex, new Generex(regex).random());
    }

    /**
     * Match a request header.
     *
     * @param header        Header to match
     * @param regex         Regular expression to match
     * @param headerExample Example value to use
     */
    public PactDslRequestWithPath matchHeader(String header, String regex, String headerExample) {
        requestMatchers.addCategory("header").setRule(header, new RegexMatcher(regex));
        requestHeaders.put(header, headerExample);
        return this;
    }

    /**
     * Define the response to return
     */
    public PactDslResponse willRespondWith() {
        return new PactDslResponse(consumerPactBuilder, this, defaultRequestValues, defaultResponseValues);
    }

    /**
     * Match a query parameter with a regex. A random query parameter value will be generated from the regex.
     *
     * @param parameter Query parameter
     * @param regex     Regular expression to match with
     */
    public PactDslRequestWithPath matchQuery(String parameter, String regex) {
        return matchQuery(parameter, regex, new Generex(regex).random());
    }

    /**
     * Match a query parameter with a regex.
     *
     * @param parameter Query parameter
     * @param regex     Regular expression to match with
     * @param example   Example value to use for the query parameter (unencoded)
     */
    public PactDslRequestWithPath matchQuery(String parameter, String regex, String example) {
        requestMatchers.addCategory("query").addRule(parameter, new RegexMatcher(regex));
        query.put(parameter, Collections.singletonList(example));
        return this;
    }

    /**
     * Match a repeating query parameter with a regex.
     *
     * @param parameter Query parameter
     * @param regex     Regular expression to match with each element
     * @param example   Example value list to use for the query parameter (unencoded)
     */
    public PactDslRequestWithPath matchQuery(String parameter, String regex, List<String> example) {
        requestMatchers.addCategory("query").addRule(parameter, new RegexMatcher(regex));
        query.put(parameter, example);
        return this;
    }

    /**
     * Sets up a file upload request. This will add the correct content type header to the request
     * @param partName This is the name of the part in the multipart body.
     * @param fileName This is the name of the file that was uploaded
     * @param fileContentType This is the content type of the uploaded file
     * @param data This is the actual file contents
     */
    public PactDslRequestWithPath withFileUpload(String partName, String fileName, String fileContentType, byte[] data)
      throws IOException {
      setupFileUpload(partName, fileName, fileContentType, data);
      return this;
    }
}
