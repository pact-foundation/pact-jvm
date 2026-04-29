// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block26
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArray;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block block26")
class README_java_block26_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:block26
        newJsonArray((rootArray) -> {
            rootArray.array((a) -> a.stringValue("a1").stringValue("a2"));
            rootArray.array((a) -> a.numberValue(1).numberValue(2));
            rootArray.array((a) -> a.object((o) -> o.stringValue("foo", "Foo")));
        }).build();
        // @DOCTEST-END
    }
}
