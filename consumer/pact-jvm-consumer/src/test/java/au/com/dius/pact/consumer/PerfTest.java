package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.RequestResponsePact;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;

public class PerfTest {

  @Test
  public void test() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    // Define the test data:
    String path = "/mypath/abc/";

    //Header data:
    Map<String, String> headerData = new HashMap<String, String>();
    headerData.put("Content-Type", "application/json");

    // Put as JSON object:
    JSONObject bodyExpected = new JSONObject();
    bodyExpected.put("name", "myName");

    stopWatch.split();
    System.out.println("Setup: " + stopWatch.getSplitTime());

    RequestResponsePact pact = ConsumerPactBuilder
      .consumer("perf_test_consumer")
      .hasPactWith("perf_test_provider")
      .uponReceiving("a request to get values")
      .path(path)
      .method("GET")
      .willRespondWith()
      .status(200)
      .headers(headerData)
      .body(bodyExpected)
      .toPact();

    stopWatch.split();
    System.out.println("Setup Fragment: " + stopWatch.getSplitTime());

    MockProviderConfig config = MockProviderConfig.createDefault();
    runConsumerTest(pact, config, (mockServer, context) -> {
      try {
        stopWatch.split();
        System.out.println("In Test: " + stopWatch.getSplitTime());
        new ConsumerClient(config.url()).getAsList(path);
      } catch (IOException e) {
      }
      stopWatch.split();
      System.out.println("After Test: " + stopWatch.getSplitTime());

      return true;
    });

    stopWatch.split();
    System.out.println("End of Test: " + stopWatch.getSplitTime());

    stopWatch.stop();
    System.out.println(stopWatch.toString());
  }

}
