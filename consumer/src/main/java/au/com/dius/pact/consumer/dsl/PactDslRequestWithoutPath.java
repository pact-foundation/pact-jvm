package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.xml.PactXmlBuilder;
import au.com.dius.pact.core.model.OptionalBody;
import au.com.dius.pact.core.model.PactReaderKt;
import au.com.dius.pact.core.model.generators.Category;
import au.com.dius.pact.core.model.generators.ProviderStateGenerator;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;
import au.com.dius.pact.core.support.expressions.DataType;
import com.mifmif.common.regex.Generex;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONObject;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
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
      for (Map.Entry<String, String> entry: headers.entrySet()) {
        this.requestHeaders.put(entry.getKey(), Collections.singletonList(entry.getValue()));
      }
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
        requestHeaders.put(firstHeaderName, Collections.singletonList(firstHeaderValue));

        for (int i = 0; i < headerNameValuePairs.length; i+=2) {
          requestHeaders.put(headerNameValuePairs[i], Collections.singletonList(headerNameValuePairs[i+1]));
        }

        return this;
    }

    /**
     * The query string for the request
     *
     * @param query query string
     */
    public PactDslRequestWithoutPath query(String query) {
        this.query = PactReaderKt.queryStringToMap(query, false);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithoutPath body(String body) {
        requestBody = OptionalBody.body(body.getBytes());
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithoutPath body(String body, String contentType) {
      return body(body, ContentType.parse(contentType));
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithoutPath body(String body, ContentType contentType) {
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
    public PactDslRequestWithoutPath body(Supplier<String> body) {
        requestBody = OptionalBody.body(body.get().getBytes());
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in Java Functional Interface Supplier that must return a string
     */
    public PactDslRequestWithoutPath body(Supplier<String> body, String contentType) {
      return this.body(body, ContentType.parse(contentType));
    }

    /**
     * The body of the request
     *
     * @param body Request body in Java Functional Interface Supplier that must return a string
     */
    public PactDslRequestWithoutPath body(Supplier<String> body, ContentType contentType) {
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
    public PactDslRequestWithoutPath bodyWithSingleQuotes(String body, String contentType) {
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
    public PactDslRequestWithoutPath bodyWithSingleQuotes(String body, ContentType contentType) {
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
    public PactDslRequestWithoutPath body(JSONObject body) {
      if (isContentTypeHeaderNotSet()) {
        requestHeaders.put(CONTENT_TYPE, Collections.singletonList(ContentType.APPLICATION_JSON.toString()));
        requestBody = OptionalBody.body(body.toString().getBytes());
      } else {
        String contentType = getContentTypeHeader();
        ContentType ct = ContentType.parse(contentType);
        Charset charset = ct.getCharset() != null ? ct.getCharset() : Charset.defaultCharset();
        requestBody = OptionalBody.body(body.toString().getBytes(charset),
          new au.com.dius.pact.core.model.ContentType(contentType));
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
      if (isContentTypeHeaderNotSet()) {
        requestHeaders.put(CONTENT_TYPE, Collections.singletonList(ContentType.APPLICATION_JSON.toString()));
        requestBody = OptionalBody.body(parent.toString().getBytes());
      } else {
        String contentType = getContentTypeHeader();
        ContentType ct = ContentType.parse(contentType);
        Charset charset = ct.getCharset() != null ? ct.getCharset() : Charset.defaultCharset();
        requestBody = OptionalBody.body(parent.toString().getBytes(charset),
          new au.com.dius.pact.core.model.ContentType(contentType));
      }
      return this;
    }

    /**
     * The body of the request
     *
     * @param body XML Document
     */
    public PactDslRequestWithoutPath body(Document body) throws TransformerException {
      if (isContentTypeHeaderNotSet()) {
        requestHeaders.put(CONTENT_TYPE, Collections.singletonList(ContentType.APPLICATION_XML.toString()));
        requestBody = OptionalBody.body(xmlToString(body).getBytes());
      } else {
        String contentType = getContentTypeHeader();
        ContentType ct = ContentType.parse(contentType);
        Charset charset = ct.getCharset() != null ? ct.getCharset() : Charset.defaultCharset();
        requestBody = OptionalBody.body(xmlToString(body).getBytes(charset),
          new au.com.dius.pact.core.model.ContentType(contentType));
      }

      return this;
    }

  /**
   * XML Response body to return
   *
   * @param xmlBuilder XML Builder used to construct the XML document
   */
  public PactDslRequestWithoutPath body(PactXmlBuilder xmlBuilder) {
    requestMatchers.addCategory(xmlBuilder.getMatchingRules());
    requestGenerators.addGenerators(xmlBuilder.getGenerators());

    if (isContentTypeHeaderNotSet()) {
      requestHeaders.put(CONTENT_TYPE, Collections.singletonList(ContentType.APPLICATION_XML.toString()));
      requestBody = OptionalBody.body(xmlBuilder.asBytes());
    } else {
      String contentType = getContentTypeHeader();
      ContentType ct = ContentType.parse(contentType);
      Charset charset = ct.getCharset() != null ? ct.getCharset() : Charset.defaultCharset();
      requestBody = OptionalBody.body(xmlBuilder.asBytes(charset),
        new au.com.dius.pact.core.model.ContentType(contentType));
    }

    return this;
  }

  /**
   * The body of the request
   *
   * @param body Built using MultipartEntityBuilder
   */
  public PactDslRequestWithoutPath body(MultipartEntityBuilder body) throws IOException {
    setupMultipart(body);
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
    requestGenerators.addGenerator(Category.HEADER, name, new ProviderStateGenerator(expression, DataType.STRING));
    requestHeaders.put(name, Collections.singletonList(example));
    return this;
  }

  /**
   * Adds a query parameter that will have it's value injected from the provider state
   * @param name Name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  public PactDslRequestWithoutPath queryParameterFromProviderState(String name, String expression, String example) {
    requestGenerators.addGenerator(Category.QUERY, name, new ProviderStateGenerator(expression, DataType.STRING));
    query.put(name, Collections.singletonList(example));
    return this;
  }

  /**
   * Sets the path to have it's value injected from the provider state
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to use in the consumer test
   */
  public PactDslRequestWithPath pathFromProviderState(String expression, String example) {
    requestGenerators.addGenerator(Category.PATH, new ProviderStateGenerator(expression, DataType.STRING));
    return new PactDslRequestWithPath(consumerPactBuilder, consumerName, providerName, pactDslWithState.state,
      description, example, requestMethod, requestHeaders, query, requestBody, requestMatchers, requestGenerators,
      defaultRequestValues, defaultResponseValues);
  }

  /**
   * Matches a date field using the provided date pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  public PactDslRequestWithoutPath queryMatchingDate(String field, String pattern, String example) {
    return (PactDslRequestWithoutPath) queryMatchingDateBase(field, pattern, example);
  }

  /**
   * Matches a date field using the provided date pattern. The current system date will be used for the example value.
   * @param field field name
   * @param pattern pattern to match
   */
  public PactDslRequestWithoutPath queryMatchingDate(String field, String pattern) {
    return (PactDslRequestWithoutPath) queryMatchingDateBase(field, pattern, null);
  }

  /**
   * Matches a time field using the provided time pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  public PactDslRequestWithoutPath queryMatchingTime(String field, String pattern, String example) {
    return (PactDslRequestWithoutPath) queryMatchingTimeBase(field, pattern, example);
  }

  /**
   * Matches a time field using the provided time pattern. The current system time will be used for the example value.
   * @param field field name
   * @param pattern pattern to match
   */
  public PactDslRequestWithoutPath queryMatchingTime(String field, String pattern) {
    return (PactDslRequestWithoutPath) queryMatchingTimeBase(field, pattern, null);
  }

  /**
   * Matches a datetime field using the provided pattern
   * @param field field name
   * @param pattern pattern to match
   * @param example Example value
   */
  public PactDslRequestWithoutPath queryMatchingDatetime(String field, String pattern, String example) {
    return (PactDslRequestWithoutPath) queryMatchingDatetimeBase(field, pattern, example);
  }

  /**
   * Matches a datetime field using the provided pattern. The current system date and time will be used for the example value.
   * @param field field name
   * @param pattern pattern to match
   */
  public PactDslRequestWithoutPath queryMatchingDatetime(String field, String pattern) {
    return (PactDslRequestWithoutPath) queryMatchingDatetimeBase(field, pattern, null);
  }

  /**
   * Matches a date field using the ISO date pattern
   * @param field field name
   * @param example Example value
   */
  public PactDslRequestWithoutPath queryMatchingISODate(String field, String example) {
    return (PactDslRequestWithoutPath) queryMatchingDateBase(field, DateFormatUtils.ISO_DATE_FORMAT.getPattern(), example);
  }

  /**
   * Matches a date field using the ISO date pattern. The current system date will be used for the example value.
   * @param field field name
   */
  public PactDslRequestWithoutPath queryMatchingISODate(String field) {
    return queryMatchingISODate(field, null);
  }

  /**
   * Matches a time field using the ISO time pattern
   * @param field field name
   * @param example Example value
   */
  public PactDslRequestWithoutPath queryMatchingISOTime(String field, String example) {
    return (PactDslRequestWithoutPath) queryMatchingTimeBase(field, DateFormatUtils.ISO_TIME_FORMAT.getPattern(), example);
  }

  /**
   * Matches a time field using the ISO time pattern. The current system time will be used for the example value.
   * @param field field name
   */
  public PactDslRequestWithoutPath queryMatchingTime(String field) {
    return queryMatchingISOTime(field, null);
  }

  /**
   * Matches a datetime field using the ISO pattern
   * @param field field name
   * @param example Example value
   */
  public PactDslRequestWithoutPath queryMatchingISODatetime(String field, String example) {
    return (PactDslRequestWithoutPath) queryMatchingDatetimeBase(field, DateFormatUtils.ISO_DATETIME_FORMAT.getPattern(),
      example);
  }

  /**
   * Matches a datetime field using the ISO pattern. The current system date and time will be used for the example value.
   * @param field field name
   */
  public PactDslRequestWithoutPath queryMatchingISODatetime(String field) {
    return queryMatchingISODatetime(field, null);
  }

}
