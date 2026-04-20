// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 8
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 8")
class README_java_block08_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:8
        new PactDslJsonBody()
          .stringType("type","FeatureCollection")
          .eachLike("features")
            .stringType("type","Feature")
            .object("geometry")
              .stringType("type","Point")
              .eachArrayLike("coordinates") // coordinates is an array of arrays 
                .decimalType(-7.55717)
                .decimalType(49.766896)
              .closeArray()
              .closeArray()
            .closeObject()
            .object("properties")
              .stringType("prop0","value0")
            .closeObject()
          .closeObject()
          .closeArray()
        // @DOCTEST-END
        ;
    }
}
