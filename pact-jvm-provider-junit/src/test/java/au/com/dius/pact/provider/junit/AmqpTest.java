package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.AmqpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.runner.RunWith;

import java.util.Collections;

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
