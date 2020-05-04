package au.com.dius.pact.provider.junit5;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import au.com.dius.pact.provider.junitsupport.State;
import com.github.tomakehurst.wiremock.WireMockServer;

public interface StateInterface2 {

  @State("state2")
  default void toState2() {
    server().stubFor(
        get(urlPathEqualTo("/moreData"))
            .willReturn(aResponse()
                .withStatus(204)));
  }

  WireMockServer server();

}
