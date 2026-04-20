// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 6
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import au.com.dius.pact.consumer.MockServer;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 6")
class README_java_block06_Test {

//    @Test
//    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:6
          @Test
          void test(MockServer mockServer) throws IOException {
            HttpResponse httpResponse = Request.get(mockServer.getUrl() + "/articles.json").execute().returnResponse();
            assertThat(httpResponse.getCode(), is(equalTo(200)));
          }
        // @DOCTEST-END
//    }
}
