package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.core.model.OptionalBody;
import au.com.dius.pact.core.model.generators.*;
import au.com.dius.pact.core.model.matchingrules.MatchingRules;
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static au.com.dius.pact.consumer.Headers.MULTIPART_HEADER_REGEX;

public abstract class PactDslRequestBase {
  protected static final String CONTENT_TYPE = "Content-Type";
  private static final long DATE_2000 = 949323600000L;

  protected final PactDslRequestWithoutPath defaultRequestValues;
  protected String requestMethod = "GET";
  protected Map<String, List<String>> requestHeaders = new HashMap<>();
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

  protected void setupFileUpload(String partName, String fileName, String fileContentType, byte[] data) throws IOException {
    HttpEntity multipart = MultipartEntityBuilder.create()
      .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
      .addBinaryBody(partName, data, ContentType.create(fileContentType), fileName)
      .build();
    OutputStream os = new ByteArrayOutputStream();
    multipart.writeTo(os);

    requestBody = OptionalBody.body(os.toString().getBytes(),
      new au.com.dius.pact.core.model.ContentType(multipart.getContentType().getValue()));
    requestMatchers.addCategory("header").addRule(CONTENT_TYPE, new RegexMatcher(MULTIPART_HEADER_REGEX,
      multipart.getContentType().getValue()));
    requestHeaders.put(CONTENT_TYPE, Collections.singletonList(multipart.getContentType().getValue()));
  }

  protected PactDslRequestBase queryMatchingDateBase(String field, String pattern, String example) {
    requestMatchers.addCategory("query").addRule(field, new au.com.dius.pact.core.model.matchingrules.DateMatcher(pattern));
    if (StringUtils.isNotEmpty(example)) {
      query.put(field, Collections.singletonList(example));
    } else {
      requestGenerators.addGenerator(Category.BODY, field, new DateGenerator(pattern, null));
      FastDateFormat instance = FastDateFormat.getInstance(pattern);
      query.put(field, Collections.singletonList(instance.format(new Date(DATE_2000))));
    }
    return this;
  }

  protected PactDslRequestBase queryMatchingTimeBase(String field, String pattern, String example) {
    requestMatchers.addCategory("query").addRule(field, new au.com.dius.pact.core.model.matchingrules.TimeMatcher(pattern));
    if (StringUtils.isNotEmpty(example)) {
      query.put(field, Collections.singletonList(example));
    } else {
      requestGenerators.addGenerator(Category.BODY, field, new TimeGenerator(pattern, null));
      FastDateFormat instance = FastDateFormat.getInstance(pattern);
      query.put(field, Collections.singletonList(instance.format(new Date(DATE_2000))));
    }
    return this;
  }

  protected PactDslRequestBase queryMatchingDatetimeBase(String field, String pattern, String example) {
    requestMatchers.addCategory("query").addRule(field, new au.com.dius.pact.core.model.matchingrules.TimestampMatcher(pattern));
    if (StringUtils.isNotEmpty(example)) {
      query.put(field, Collections.singletonList(example));
    } else {
      requestGenerators.addGenerator(Category.BODY, field, new DateTimeGenerator(pattern, null));
      FastDateFormat instance = FastDateFormat.getInstance(pattern);
      query.put(field, Collections.singletonList(instance.format(new Date(DATE_2000))));
    }
    return this;
  }
}
