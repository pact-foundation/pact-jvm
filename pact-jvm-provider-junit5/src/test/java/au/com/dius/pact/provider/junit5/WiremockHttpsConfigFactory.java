package au.com.dius.pact.provider.junit5;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import ru.lanwen.wiremock.config.WiremockConfigFactory;

public class WiremockHttpsConfigFactory implements WiremockConfigFactory {
  @Override
  public WireMockConfiguration create() {
    return WireMockConfiguration.options().dynamicHttpsPort().notifier(new Slf4jNotifier(true));
  }
}
