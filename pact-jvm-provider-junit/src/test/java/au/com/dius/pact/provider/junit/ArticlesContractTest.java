package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.Charset;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;

@RunWith(PactRunner.class)
@Provider("ArticlesProvider")
@PactFolder("../pact-jvm-consumer-junit/build/2.11/pacts")
public class ArticlesContractTest {
  @TestTarget
  public final Target target = new HttpTarget(8000);

  @ClassRule
  public static final ClientDriverRule embeddedService = new ClientDriverRule(8000);

  @Before
  public void before() throws IOException {
    String json = IOUtils.toString(getClass().getResourceAsStream("/articles.json"), Charset.defaultCharset());
    embeddedService.addExpectation(
      onRequestTo("/articles.json"), giveResponse(json, "application/json")
    );
  }
}
