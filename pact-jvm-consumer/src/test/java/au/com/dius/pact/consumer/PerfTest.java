package au.com.dius.pact.consumer;

import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactFragment;
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
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    // Define the test data:
    final String path = "/mypath/abc/";

    //Header data:
    Map<String, String> headerData = new HashMap<String, String>();
    headerData.put("Content-Type", "application/json");

    // Put as JSON object:
    JSONObject bodyExpected = new JSONObject();
    bodyExpected.put("name", "myName");

    stopWatch.split();
    System.out.println("Setup: " + stopWatch.getSplitTime());

    PactFragment pactFragment = ConsumerPactBuilder
      .consumer("perf_test_consumer")
      .hasPactWith("perf_test_provider")
      .uponReceiving("a request to get values")
      .path(path)
      .method("GET")
      .willRespondWith()
      .status(200)
      .headers(headerData)
      .body(bodyExpected)
      .toFragment();

    stopWatch.split();
    System.out.println("Setup Fragment: " + stopWatch.getSplitTime());

    final MockProviderConfig config = MockProviderConfig.createDefault();
    PactVerificationResult result = runConsumerTest(pactFragment.toPact(), config, new PactTestRun() {
      @Override
      public void run(@NotNull MockServer mockServer) throws IOException {
        try {
          stopWatch.split();
          System.out.println("In Test: " + stopWatch.getSplitTime());
          new ConsumerClient(config.url()).getAsList(path);
        } catch (IOException e) {
        }
        stopWatch.split();
        System.out.println("After Test: " + stopWatch.getSplitTime());
      }
    });

    stopWatch.split();
    System.out.println("End of Test: " + stopWatch.getSplitTime());

    stopWatch.stop();
    System.out.println(stopWatch.toString());
  }

}
