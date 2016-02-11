package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactReader;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static au.com.dius.pact.provider.junit.sysprops.PactRunnerExpressionParser.parseExpressions;

/**
 * Out-of-the-box implementation of {@link PactLoader} that downloads pacts from Pact broker
 */
public class PactBrokerLoader implements PactLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PactBrokerLoader.class);
    private static final String PACT_URL_PATTERN = "/pacts/provider/{0}/latest";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().disable(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final String pactBrokerHost;
    private final String pactBrokerPort;
    private final String pactBrokerProtocol;
    private final Retryer<HttpResponse> retryer = RetryerBuilder.<HttpResponse>newBuilder()
            .retryIfResult(new Predicate<HttpResponse>() {
                @Override
                public boolean apply(@Nullable HttpResponse response) {
                    return response.getStatusLine().getStatusCode() >= 500;
                }
            })
            .withWaitStrategy(WaitStrategies.exponentialWait(100, 1, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterDelay(5000))
            .build();
  private Callable<HttpResponse> httpResponseCallable;

    public PactBrokerLoader(final String pactBrokerHost, final String pactBrokerPort, final String pactBrokerProtocol) {
        this.pactBrokerHost = pactBrokerHost;
        this.pactBrokerPort = pactBrokerPort;
        this.pactBrokerProtocol = pactBrokerProtocol;
    }

    public PactBrokerLoader(final PactBroker pactBroker) {
        this(pactBroker.host(), pactBroker.port(), pactBroker.protocol());
    }

    public List<Pact> load(final String providerName) throws IOException {
        final HttpResponse httpResponse;
        try {
          final URI brokerUri = new URIBuilder().setScheme(parseExpressions(pactBrokerProtocol))
            .setHost(parseExpressions(pactBrokerHost))
            .setPort(Integer.parseInt(parseExpressions(pactBrokerPort)))
            .setPath(MessageFormat.format(PACT_URL_PATTERN, providerName))
            .build();
          if (httpResponseCallable == null) {
            httpResponseCallable = new Callable<HttpResponse>() {
              @Override
              public HttpResponse call() throws Exception {
                return Request.Get(brokerUri)
                  .setHeader(HttpHeaders.ACCEPT, "application/hal+json")
                  .execute().returnResponse();
              }
            };
          }
          httpResponse = retryer.call(httpResponseCallable);
        } catch (final Exception e) {
            throw new IOException("Was not able load pacts from broker", e);
        }

      final int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode == 404) {
            LOGGER.warn("There are no pacts found for the service '" + providerName + "'");
            return Collections.emptyList();
        }

        if (statusCode / 100 != 2) {
            throw new RuntimeException("Pact broker responded with status: " + statusCode +
                    "\n payload: '" + IOUtils.toString(httpResponse.getEntity().getContent()) + "'");
        }

        final JsonNode fullList = OBJECT_MAPPER.readTree(httpResponse.getEntity().getContent());
        JsonNode path = fullList.path("_links").path("pacts");
        List<Pact> pacts = new ArrayList<Pact>();
        for (JsonNode jsonNode: path) {
            pacts.add(PactReader.loadPact(jsonNode.get("href").asText()));
        }
        return pacts;
    }

  public Callable<HttpResponse> getHttpResponseCallable() {
    return httpResponseCallable;
  }

  public void setHttpResponseCallable(Callable<HttpResponse> httpResponseCallable) {
    this.httpResponseCallable = httpResponseCallable;
  }
}
