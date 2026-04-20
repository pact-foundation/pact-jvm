// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 12
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.junit5.doctest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 12")
class README_java_block12_Test {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN README.md:java:12
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
    }
}
