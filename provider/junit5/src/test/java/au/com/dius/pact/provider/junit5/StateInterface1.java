package au.com.dius.pact.provider.junit5;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import au.com.dius.pact.provider.junitsupport.State;
import com.github.tomakehurst.wiremock.WireMockServer;

public interface StateInterface1 {

  @State("state1")
  default void toState1() {
    server().stubFor(
        get(urlPathEqualTo("/data"))
            .willReturn(aResponse()
                .withStatus(204)));
  }

  WireMockServer server();

}
