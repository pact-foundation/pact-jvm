package au.com.dius.pact.provider.junit5;

import com.github.tomakehurst.wiremock.WireMockServer;

public class StateClass1 implements StateInterface1 {
  private final WireMockServer server;

  public StateClass1(WireMockServer server) {
    this.server = server;
  }

  @Override
  public WireMockServer server() {
    return server;
  }
}
