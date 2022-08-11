package au.com.dius.pact.provider.spring.testspringbootapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TestApplication {

  public static class ObjectThatMustBeClosed {
    private ObjectThatMustBeClosed() {}

    private static final ObjectThatMustBeClosed instance = new ObjectThatMustBeClosed();
    public static ObjectThatMustBeClosed getInstance() {
      return instance;
    }

    public boolean destroyed = false;

    public void shutdown() {
      destroyed = true;
    }
  }

  @Bean(destroyMethod="shutdown")
  ObjectThatMustBeClosed mustBeClosed() {
    return ObjectThatMustBeClosed.getInstance();
  }
}
