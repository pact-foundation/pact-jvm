package au.com.dius.pact.provider.spring.junit5;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Provider("myAwesomeService")
@PactFolder("pacts")
public class WebfluxPactTest {

  public static class Handler {
    public Mono<ServerResponse> handleRequest(ServerRequest request) {
      return ServerResponse.noContent().build();
    }
  }

  static class Router {
    public RouterFunction<ServerResponse> route(Handler handler) {
      return RouterFunctions
        .route(RequestPredicates.GET("/data").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
          handler::handleRequest)
        .andRoute(RequestPredicates.GET("/async-data").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
          handler::handleRequest);
    }
  }

  @BeforeEach
  void setup(PactVerificationContext context) {
    Handler handler = new Handler();
    context.setTarget(new WebFluxTarget(new Router().route(handler)));
  }

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }
}
