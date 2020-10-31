package au.com.dius.pact.consumer.junit;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.support.json.JsonParser;
import au.com.dius.pact.core.support.json.JsonValue;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.httpkit.HttpMethod;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Defect464Test {

  private static final String JSON_ARRAY_MEMBER_NAME = "my-array";
  private static final String PROVIDER_NAME = "464_provider";
  private static final String PROVIDER_URI = "/provider/uri";

  @Rule
  public PactProviderRule provider = new PactProviderRule(PROVIDER_NAME, this);

  @Pact(provider = PROVIDER_NAME, consumer = "test_consumer")
  public RequestResponsePact createFragment(PactDslWithProvider builder) {
    final DslPart body = new PactDslJsonBody()
      .minArrayLike("my-array", 2)
        .stringType("id")
        .closeObject()
      .closeArray();

    return builder
      .uponReceiving("a request for a json-array")
      .path(PROVIDER_URI)
      .method(HttpMethod.GET.toString())
      .willRespondWith()
      .status(HttpStatus.SC_OK)
      .body(body)
      .toPact();
  }

  @Test
  @PactVerification(PROVIDER_NAME)
  public void runTest() throws IOException {
    String jsonString
      = Request.Get(provider.getUrl() + PROVIDER_URI).execute().returnContent().asString();
    JsonValue root = JsonParser.parseString(jsonString);
    JsonValue.Array myArrayElement = root.asObject().get(JSON_ARRAY_MEMBER_NAME).asArray();
    List<ElementOfMyArray> myArray = myArrayElement.getValues().stream()
      .map(e -> new ElementOfMyArray(e.get("id").asString()))
      .collect(Collectors.toList());

    List<String> ids = new ArrayList<>();
    for (ElementOfMyArray elementOfMyArray : myArray) {
      String elementOfMyArrayId = elementOfMyArray.getId();

      Assert.assertFalse(
        "Id " + elementOfMyArrayId + " is already known.", ids.contains(elementOfMyArrayId)
      );

      ids.add(elementOfMyArrayId);
    }
  }

  private static class ElementOfMyArray {

    String id;

    public ElementOfMyArray(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }
}
