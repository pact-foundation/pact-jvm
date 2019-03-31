package au.com.dius.pact.provider.junit;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;

import com.github.restdriver.clientdriver.ClientDriverRule;

public interface StateInterface1 {

  @State("state1")
  default void toState1(){
    embeddedProvider().addExpectation(
        onRequestTo("/data"), giveEmptyResponse()
    );
  }

  ClientDriverRule embeddedProvider();

}
