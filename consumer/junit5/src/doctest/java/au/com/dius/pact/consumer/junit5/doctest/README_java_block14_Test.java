// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block block14
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Disabled("Doctest stub — see README.md block block14")
class README_java_block14_Test {

  class CSVRecord {

    public String get(int i) {
      return null;
    }
  }

  class CsvClient {

    public CsvClient(@NotNull String url) {
    }

    public List<CSVRecord> fetch(String s, boolean b) {
      return null;
    }
  }

//    @Test
//    void block() throws Exception {
        @Nested
        // @DOCTEST-BEGIN README.md:java:block14
        @PactConsumerTest
        class CsvClientTest {
          /**
           * Setup an interaction that makes a request for a CSV report 
           */
          @Pact(consumer = "CsvClient")
          V4Pact pact(PactBuilder builder) {
            return builder
              // Tell the builder to load the CSV plugin
              .usingPlugin("csv")
              // Interaction we are expecting to receive
              .expectsToReceive("request for a report", "core/interaction/http")
              // Data for the interaction. This will be sent to the plugin
              .with(Map.of(
                "request.path", "/reports/report001.csv",
                "response.status", "200",
                "response.contents", Map.of(
                  "pact:content-type", "text/csv",
                  "csvHeaders", false,
                  "column:1", "matching(type,'Name')",
                  "column:2", "matching(number,100)",
                  "column:3", "matching(datetime, 'yyyy-MM-dd','2000-01-01')"
                )
              ))
              .toPact();
          }
        
          /**
           * Test to get the CSV report
           */
          @Test
          @PactTestFor(providerName = "CsvServer", pactMethod = "pact")
          void getCsvReport(MockServer mockServer) throws IOException {
            // Setup our CSV client class to point to the Pact mock server
            CsvClient client = new CsvClient(mockServer.getUrl());
            
            // Fetch the CSV report
            List<CSVRecord> csvData = client.fetch("report001.csv", false);
            
            // Verify it is as expected
            assertThat(csvData.size(), is(1));
            assertThat(csvData.get(0).get(0), is(equalTo("Name")));
            assertThat(csvData.get(0).get(1), is(equalTo("100")));
            assertThat(csvData.get(0).get(2), matchesRegex("\\d{4}-\\d{2}-\\d{2}"));
          }
        }
        // @DOCTEST-END
//    }
}
