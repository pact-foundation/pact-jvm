package au.com.dius.pact.consumer.dsl;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

class DslTest {

  @Test
  void arrayWithJsonPrimitives() {
    /*
    [
      "01/01/2019",
      "01/02/2019",
      "01/03/2019"
    ]
    */

    // Old DSL
    DslPart oldDsl = PactDslJsonArray.arrayMinLike(1, 3,
      PactDslJsonRootValue.stringMatcher("^(([0-3]?\\d+)\\/((0?[1-9])|(1[0-2]))\\/20\\d{2})$",
        "01/01/2019"));

    // New DSL
    DslPart newDsl = Dsl.arrayOfPrimitives()
      .withMinLength(1)
      .withNumberOfExamples(3)
      .thatMatchRegex("^(([0-3]?\\d+)\\/((0?[1-9])|(1[0-2]))\\/20\\d{2})$", "01/01/2019")
      .build();

    assertThat(newDsl.toString(), is(oldDsl.toString()));
    assertThat(newDsl.getMatchers(), is(oldDsl.getMatchers()));
    assertThat(newDsl.getGenerators(), is(oldDsl.getGenerators()));
  }

}
