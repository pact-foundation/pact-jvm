// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block07
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Date;

@Disabled("Doctest stub — see README.md block block07")
class README_java_block07_Test {

    @Test
    void block() throws Exception {
        Date date = new Date();
        // @DOCTEST-BEGIN README.md:java:block07
        PactDslJsonArray.arrayEachLike()
            .date("clearedDate", "mm/dd/yyyy", date)
            .stringType("status", "STATUS")
            .decimalType("amount", 100.0)
        .closeObject()
        // @DOCTEST-END
        ;
    }
}
