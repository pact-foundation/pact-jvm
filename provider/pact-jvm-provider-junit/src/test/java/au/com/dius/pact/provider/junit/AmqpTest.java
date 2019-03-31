package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.AmqpTarget;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import groovy.json.JsonOutput;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;

@RunWith(PactRunner.class)
@Provider("AmqpProvider")
@PactFolder("src/test/resources/amqp_pacts")
public class AmqpTest {
  @TestTarget
  public final Target target = new AmqpTarget(Collections.singletonList("au.com.dius.pact.provider.junit.*"));

  @State("SomeProviderState")
  public void someProviderState() {}

  @PactVerifyProvider("a test message")
  public String verifyMessageForOrder() {
    return "{\"testParam1\": \"value1\",\"testParam2\": \"value2\"}";
  }
}
