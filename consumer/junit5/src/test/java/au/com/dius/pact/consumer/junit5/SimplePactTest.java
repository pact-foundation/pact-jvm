package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.*;

import java.util.List;
import java.util.Objects;

@ExtendWith({PactConsumerTestExt.class, SamplePactTest.MyStringResolverExtension.class})
@PactTestFor(providerName = "some-provider", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
class SamplePactTest {

  @BeforeAll
  static void setup(String injectedString) {
    System.out.println(injectedString);
  }

  @Pact(consumer = "some-consumer")
  public MessagePact someMessage(MessagePactBuilder builder) {
    return builder
      .expectsToReceive("message")
      .withContent(LambdaDsl.newJsonBody(object -> object.stringType("test", "Test")).build()).toPact();
  }

  @Test
  @PactTestFor(pactMethod = "someMessage")
  void consume(List<Message> messages) {
    System.out.println(messages);
  }

  static class MyStringResolverExtension implements Extension, ParameterResolver {
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
      return Objects.equals(parameterContext.getParameter().getType(), String.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
      return "injectedString";
    }
  }
}
