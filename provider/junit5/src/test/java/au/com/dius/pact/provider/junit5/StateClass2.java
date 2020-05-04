package au.com.dius.pact.provider.junit5;

import com.github.tomakehurst.wiremock.WireMockServer;

public class StateClass2 implements StateInterface2 {
  private final WireMockServer server;

  public StateClass2(WireMockServer server) {
    this.server = server;
  }

  @Override
  public WireMockServer server() {
    return server;
  }
}
