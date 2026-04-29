// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block19
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.LambdaDsl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Doctest stub — see README.md block block19")
class README_java_block19_Test {

    @Test
    void block() throws Exception {
      LambdaDsl.newJsonArray(array -> {
        // @DOCTEST-BEGIN README.md:java:block19
        array.object((o) -> {
          o.stringValue("foo", "Foo");          // an attribute
          o.stringValue("bar", "Bar");          // an attribute
          o.object("tar", (tarObject) -> {      // an attribute with a nested object
            tarObject.stringValue("a", "A");    // attribute of the nested object
            tarObject.stringValue("b", "B");    // attribute of the nested object
          });
        });
        // @DOCTEST-END
      });
    }
}
