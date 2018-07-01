package au.com.dius.pact.provider.spring.testspringbootapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class TestApplication {

  @Singleton
  class ObjectThatMustBeClosed {
    boolean destroyed = false

    def shutdown() {
      destroyed = true
    }
  }

  @Bean(destroyMethod= 'shutdown')
  ObjectThatMustBeClosed mustBeClosed() {
    ObjectThatMustBeClosed.instance
  }
}
