package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactReader;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * Out-of-the-box implementation of {@link PactLoader} that downloads pacts from Pact broker
 */
public class PactBrokerLoader implements PactLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PactBrokerLoader.class);
    private static final String PACT_URL_PATTERN = "{0}://{1}:{2}/pacts/provider/{3}/latest";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().disable(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final String pactBrokerHost;
    private final int pactBrokerPort;
    private final String pactBrokerProtocol;
    private final Retryer<HttpResponse> retryer = RetryerBuilder.<HttpResponse>newBuilder()
            .retryIfResult(response -> response.getStatusLine().getStatusCode() >= 500)
            .withWaitStrategy(WaitStrategies.exponentialWait(100, 1, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterDelay(5000))
            .build();

    public PactBrokerLoader(final String pactBrokerHost, final int pactBrokerPort, final String pactBrokerProtocol) {
        this.pactBrokerHost = pactBrokerHost;
        this.pactBrokerPort = pactBrokerPort;
        this.pactBrokerProtocol = pactBrokerProtocol;
    }

    public PactBrokerLoader(final PactBroker pactBroker) {
        this(pactBroker.host(), pactBroker.port(), pactBroker.protocol());
    }

    public List<Pact> load(final String providerName) throws IOException {
        final String uri = MessageFormat.format(PACT_URL_PATTERN, pactBrokerProtocol, pactBrokerHost,
          Integer.toString(pactBrokerPort), providerName);
        final HttpResponse httpResponse;
        try {
            httpResponse = retryer.call(() -> Request.Get(uri)
                    .setHeader(HttpHeaders.ACCEPT, "application/hal+json")
                    .execute().returnResponse());
        } catch (final ExecutionException | RetryException e) {
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
        return StreamSupport.stream(fullList.path("_links").path("pacts").spliterator(), false)
                .map(jsonNode -> jsonNode.get("href").asText())
                .map(PactReader::loadPact)
                .map(obj -> (Pact) obj)
                .collect(toList());
    }
}
