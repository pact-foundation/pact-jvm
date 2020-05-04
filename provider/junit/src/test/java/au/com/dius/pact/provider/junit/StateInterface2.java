package au.com.dius.pact.provider.junit;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;

import com.github.restdriver.clientdriver.ClientDriverRule;

public interface StateInterface2 {

  @State("state2")
  default void toState2(){
    embeddedProvider().addExpectation(
        onRequestTo("/moreData"), giveEmptyResponse()
    );
  }

  ClientDriverRule embeddedProvider();

}
