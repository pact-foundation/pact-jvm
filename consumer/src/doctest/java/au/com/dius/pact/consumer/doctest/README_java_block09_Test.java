// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 9
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 9")
class README_java_block09_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:9
        DslPart body = new PactDslJsonBody()
          .object("one")
            .eachKeyLike("001", PactDslJsonRootValue.id(12345L)) // key like an id mapped to a matcher
          .closeObject()
          .object("two")
            .eachKeyLike("001-A") // key like an id where the value is matched by the following example
              .stringType("description", "Some Description")
            .closeObject()
          .closeObject()
          .object("three")
            .eachKeyMappedToAnArrayLike("001") // key like an id mapped to an array where each item is matched by the following example
              .id("someId", 23456L)
              .closeObject()
            .closeArray()
          .closeObject();
        // @DOCTEST-END
    }
}
