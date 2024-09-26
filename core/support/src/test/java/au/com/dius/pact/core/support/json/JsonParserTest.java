package au.com.dius.pact.core.support.json;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class JsonParserTest {
  @Test
  void canParseRawVersionSelectors() {
    String jsonStr = "[{\"mainBranch\": true}, {\"deployedOrReleased\": true}, {\"matchingBranch\": true}]";
    JsonValue jsonValue = JsonParser.parseString(jsonStr);

    JsonValue json = new JsonValue.Array(List.of(
      new JsonValue.Object(Map.of("mainBranch", JsonValue.True.INSTANCE)),
      new JsonValue.Object(Map.of("deployedOrReleased", JsonValue.True.INSTANCE)),
      new JsonValue.Object(Map.of("matchingBranch", JsonValue.True.INSTANCE))
    ));
    assertThat(jsonValue, is(equalTo(json)));
  }
}
