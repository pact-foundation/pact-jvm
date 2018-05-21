package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.Headers;
import au.com.dius.pact.model.OptionalBody;
import au.com.dius.pact.model.generators.Generators;
import au.com.dius.pact.model.matchingrules.MatchingRules;
import au.com.dius.pact.model.matchingrules.MatchingRulesImpl;
import au.com.dius.pact.model.matchingrules.RegexMatcher;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PactDslRequestBase {
  protected static final String CONTENT_TYPE = "Content-Type";

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

  protected void setupFileUpload(String partName, String fileName, String fileContentType, byte[] data) throws IOException {
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
  }
}
