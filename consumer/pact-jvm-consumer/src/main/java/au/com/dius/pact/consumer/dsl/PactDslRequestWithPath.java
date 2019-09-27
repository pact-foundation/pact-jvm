package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.core.model.Consumer;
import au.com.dius.pact.core.model.OptionalBody;
import au.com.dius.pact.core.model.PactReaderKt;
import au.com.dius.pact.core.model.Provider;
import au.com.dius.pact.core.model.ProviderState;
import au.com.dius.pact.core.model.generators.Category;
import au.com.dius.pact.core.model.generators.Generators;
import au.com.dius.pact.core.model.generators.ProviderStateGenerator;
import au.com.dius.pact.core.model.matchingrules.MatchingRules;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;
import com.mifmif.common.regex.Generex;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
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
                        Map<String, List<String>> requestHeaders,
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
    this.path = existing.path;

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
        requestHeaders.put(firstHeaderName, Collections.singletonList(firstHeaderValue));

        for (int i = 0; i < headerNameValuePairs.length; i+=2) {
          requestHeaders.put(headerNameValuePairs[i], Collections.singletonList(headerNameValuePairs[i+1]));
        }

        return this;
    }

    /**
     * Headers to be included in the request
     *
     * @param headers Key-value pairs
     */
    public PactDslRequestWithPath headers(Map<String, String> headers) {
      for (Map.Entry<String, String> entry: headers.entrySet()) {
        requestHeaders.put(entry.getKey(), Collections.singletonList(entry.getValue()));
      }
      return this;
    }

    /**
     * The query string for the request
     *
     * @param query query string
     */
    public PactDslRequestWithPath query(String query) {
        this.query = PactReaderKt.queryStringToMap(query, false);
        return this;
    }

    /**
     * The encoded query string for the request
     *
     * @param query query string
     */
    public PactDslRequestWithPath encodedQuery(String query) {
        this.query = PactReaderKt.queryStringToMap(query, true);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithPath body(String body) {
        requestBody = OptionalBody.body(body.getBytes());
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithPath body(String body, String contentType) {
      return body(body, ContentType.parse(contentType));
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithPath body(String body, ContentType contentType) {
      Charset charset = contentType.getCharset() == null ? Charset.defaultCharset() : contentType.getCharset();
      requestBody = OptionalBody.body(body.getBytes(charset), new au.com.dius.pact.core.model.ContentType(contentType.toString()));
      requestHeaders.put(CONTENT_TYPE, Collections.singletonList(contentType.toString()));
      return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in Java Functional Interface Supplier that must return a string
     */
    public PactDslRequestWithPath body(Supplier<String> body) {
        requestBody = OptionalBody.body(body.get().getBytes());
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in Java Functional Interface Supplier that must return a string
     */
    public PactDslRequestWithPath body(Supplier<String> body, String contentType) {
      return body(body, ContentType.parse(contentType));
    }

    /**
     * The body of the request
     *
     * @param body Request body in Java Functional Interface Supplier that must return a string
     */
    public PactDslRequestWithPath body(Supplier<String> body, ContentType contentType) {
      Charset charset = contentType.getCharset() == null ? Charset.defaultCharset() : contentType.getCharset();
      requestBody = OptionalBody.body(body.get().getBytes(charset), new au.com.dius.pact.core.model.ContentType(contentType.toString()));
      requestHeaders.put(CONTENT_TYPE, Collections.singletonList(contentType.toString()));
      return this;
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
    public PactDslRequestWithPath bodyWithSingleQuotes(String body, String contentType) {
      if (body != null) {
        body = QuoteUtil.convert(body);
      }
      return body(body, contentType);
    }

    /**
     * The body of the request with possible single quotes as delimiters
     * and using {@link QuoteUtil} to convert single quotes to double quotes if required.
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithPath bodyWithSingleQuotes(String body, ContentType contentType) {
      if (body != null) {
        body = QuoteUtil.convert(body);
      }
      return body(body, contentType);
    }

    /**
     * The body of the request
     *
     * @param body Request body in JSON form
     */
    public PactDslRequestWithPath body(JSONObject body) {
        requestBody = OptionalBody.body(body.toString().getBytes(),
          au.com.dius.pact.core.model.ContentType.Companion.getJSON());
        if (!requestHeaders.containsKey(CONTENT_TYPE)) {
            requestHeaders.put(CONTENT_TYPE, Collections.singletonList(ContentType.APPLICATION_JSON.toString()));
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

        if (parent instanceof PactDslJsonRootValue) {
          ((PactDslJsonRootValue)parent).setEncodeJson(true);
        }

        requestMatchers.addCategory(parent.getMatchers());
        requestGenerators.addGenerators(parent.generators);
        if (parent.getBody() != null) {
          requestBody = OptionalBody.body(parent.getBody().toString().getBytes(),
            au.com.dius.pact.core.model.ContentType.Companion.getJSON());
        } else {
          requestBody = OptionalBody.nullBody();
        }
        if (!requestHeaders.containsKey(CONTENT_TYPE)) {
            requestHeaders.put(CONTENT_TYPE, Collections.singletonList(ContentType.APPLICATION_JSON.toString()));
        }
        return this;
    }

    /**
     * The body of the request
     *
     * @param body XML Document
     */
    public PactDslRequestWithPath body(Document body) throws TransformerException {
        requestBody = OptionalBody.body(ConsumerPactBuilder.xmlToString(body).getBytes(),
          new au.com.dius.pact.core.model.ContentType(ContentType.APPLICATION_XML.toString()));
        if (!requestHeaders.containsKey(CONTENT_TYPE)) {
            requestHeaders.put(CONTENT_TYPE, Collections.singletonList(ContentType.APPLICATION_XML.toString()));
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
        requestHeaders.put(header, Collections.singletonList(headerExample));
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

  /**
   * Adds a header that will have it's value injected from the provider state
   * @param name Header Name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  public PactDslRequestWithPath headerFromProviderState(String name, String expression, String example) {
    requestGenerators.addGenerator(Category.HEADER, name, new ProviderStateGenerator(expression));
    requestHeaders.put(name, Collections.singletonList(example));
    return this;
  }

  /**
   * Adds a query parameter that will have it's value injected from the provider state
   * @param name Name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  public PactDslRequestWithPath queryParameterFromProviderState(String name, String expression, String example) {
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
    this.path = example;
    return this;
  }

  /**
   * Matches a date field using the provided date pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  public PactDslRequestWithPath queryMatchingDate(String field, String pattern, String example) {
    return (PactDslRequestWithPath) queryMatchingDateBase(field, pattern, example);
  }

  /**
   * Matches a date field using the provided date pattern. The current system date will be used for the example value.
   * @param field field name
   * @param pattern pattern to match
   */
  public PactDslRequestWithPath queryMatchingDate(String field, String pattern) {
    return (PactDslRequestWithPath) queryMatchingDateBase(field, pattern, null);
  }

  /**
   * Matches a time field using the provided time pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  public PactDslRequestWithPath queryMatchingTime(String field, String pattern, String example) {
    return (PactDslRequestWithPath) queryMatchingTimeBase(field, pattern, example);
  }

  /**
   * Matches a time field using the provided time pattern. The current system time will be used for the example value.
   * @param field field name
   * @param pattern pattern to match
   */
  public PactDslRequestWithPath queryMatchingTime(String field, String pattern) {
    return (PactDslRequestWithPath) queryMatchingTimeBase(field, pattern, null);
  }

  /**
   * Matches a datetime field using the provided pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  public PactDslRequestWithPath queryMatchingDatetime(String field, String pattern, String example) {
    return (PactDslRequestWithPath) queryMatchingDatetimeBase(field, pattern, example);
  }

  /**
   * Matches a datetime field using the provided pattern. The current system date and time will be used for the example value.
   * @param field field name
   * @param pattern pattern to match
   */
  public PactDslRequestWithPath queryMatchingDatetime(String field, String pattern) {
    return (PactDslRequestWithPath) queryMatchingDatetimeBase(field, pattern, null);
  }

  /**
   * Matches a date field using the ISO date pattern
   * @param field field name
   * @param example Example value
   */
  public PactDslRequestWithPath queryMatchingISODate(String field, String example) {
    return (PactDslRequestWithPath) queryMatchingDateBase(field, DateFormatUtils.ISO_DATE_FORMAT.getPattern(), example);
  }

  /**
   * Matches a date field using the ISO date pattern. The current system date will be used for the example value.
   * @param field field name
   */
  public PactDslRequestWithPath queryMatchingISODate(String field) {
    return queryMatchingISODate(field, null);
  }

  /**
   * Matches a time field using the ISO time pattern
   * @param field field name
   * @param example Example value
   */
  public PactDslRequestWithPath queryMatchingISOTime(String field, String example) {
    return (PactDslRequestWithPath) queryMatchingTimeBase(field, DateFormatUtils.ISO_TIME_NO_T_FORMAT.getPattern(),
      example);
  }

  /**
   * Matches a time field using the ISO time pattern. The current system time will be used for the example value.
   * @param field field name
   */
  public PactDslRequestWithPath queryMatchingTime(String field) {
    return queryMatchingISOTime(field, null);
  }

  /**
   * Matches a datetime field using the ISO pattern
   * @param field field name
   * @param example Example value
   */
  public PactDslRequestWithPath queryMatchingISODatetime(String field, String example) {
    return (PactDslRequestWithPath) queryMatchingDatetimeBase(field, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern(),
      example);
  }

  /**
   * Matches a datetime field using the ISO pattern. The current system date and time will be used for the example value.
   * @param field field name
   */
  public PactDslRequestWithPath queryMatchingISODatetime(String field) {
    return queryMatchingISODatetime(field, null);
  }
}
