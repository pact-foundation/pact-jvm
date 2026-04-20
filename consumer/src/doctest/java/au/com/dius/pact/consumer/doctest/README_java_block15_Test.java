// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 15
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 15")
class README_java_block15_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:15
        new PactDslJsonArray()
            .array()                            // open an array
            .stringValue("a1")                  // choose the method that is valid for arrays
            .stringValue("a2")                  // choose the method that is valid for arrays
            .closeArray()                       // close the array
            .array()                            // open an array
            .numberValue(1)                     // choose the method that is valid for arrays
            .numberValue(2)                     // choose the method that is valid for arrays
            .closeArray()                       // close the array
            .array()                            // open an array
            .object()                           // now we work with an object
            .stringValue("foo", "Foo")          // choose the method that is valid for objects
            .closeObject()                      // close the object and we're back in the array
            .closeArray()                       // close the array
        // @DOCTEST-END
        ;
    }
}
