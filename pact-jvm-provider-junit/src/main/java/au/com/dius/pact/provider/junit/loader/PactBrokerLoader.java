package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.Consumer;
import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactBrokerSource;
import au.com.dius.pact.model.PactReader;
import au.com.dius.pact.model.PactSource;
import au.com.dius.pact.pactbroker.PactBrokerConsumer;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.provider.broker.PactBrokerClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static au.com.dius.pact.provider.junit.sysprops.PactRunnerExpressionParser.parseExpression;
import static au.com.dius.pact.provider.junit.sysprops.PactRunnerExpressionParser.parseListExpression;

/**
 * Out-of-the-box implementation of {@link PactLoader} that downloads pacts from Pact broker
 */
public class PactBrokerLoader implements PactLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(PactBrokerLoader.class);
  private static final String LATEST = "latest";

  private final String pactBrokerHost;
  private final String pactBrokerPort;
  private final String pactBrokerProtocol;
  private final List<String> pactBrokerTags;
  private boolean failIfNoPactsFound;
  private PactBrokerAuth authentication;
  private PactBrokerSource pactSource;

  public PactBrokerLoader(final String pactBrokerHost, final String pactBrokerPort, final String pactBrokerProtocol) {
      this(pactBrokerHost, pactBrokerPort, pactBrokerProtocol, Collections.singletonList(LATEST));
  }

  public PactBrokerLoader(final String pactBrokerHost, final String pactBrokerPort, final String pactBrokerProtocol,
                          final List<String> tags) {
    this.pactBrokerHost = pactBrokerHost;
    this.pactBrokerPort = pactBrokerPort;
    this.pactBrokerProtocol = pactBrokerProtocol;
    this.pactBrokerTags = processTagExpressions(tags);
    this.failIfNoPactsFound = true;
    this.pactSource = new PactBrokerSource(this.pactBrokerHost, this.pactBrokerPort);
  }

  private static List<String> processTagExpressions(List<String> tags) {
    List<String> processedTags = new ArrayList();
    for (String tag: tags) {
      processedTags.addAll(parseListExpression(tag));
    }
    return processedTags;
  }

  public PactBrokerLoader(final PactBroker pactBroker) {
      this(pactBroker.host(), pactBroker.port(), pactBroker.protocol(), Arrays.asList(pactBroker.tags()));
      this.failIfNoPactsFound = pactBroker.failIfNoPactsFound();
      this.authentication = pactBroker.authentication();
  }

  public List<Pact> load(final String providerName) throws IOException {
    List<Pact> pacts = new ArrayList<>();
    if (pactBrokerTags == null || pactBrokerTags.isEmpty() || pactBrokerTags.size() == 1 &&
      pactBrokerTags.contains(LATEST)) {
      pacts.addAll(loadPactsForProvider(providerName, null));
    } else {
      for (String tag : pactBrokerTags) {
        pacts.addAll(loadPactsForProvider(providerName, tag));
      }
    }
    return pacts;
  }

  @Override
  public PactSource getPactSource() {
    return pactSource;
  }

  private List<Pact> loadPactsForProvider(final String providerName, final String tag) throws IOException {
    LOGGER.debug("Loading pacts from pact broker for provider " + providerName + " and tag " + tag);
    URIBuilder uriBuilder = new URIBuilder().setScheme(parseExpression(pactBrokerProtocol))
      .setHost(parseExpression(pactBrokerHost))
      .setPort(Integer.parseInt(parseExpression(pactBrokerPort)));
    try {
      List<ConsumerInfo> consumers = new ArrayList<>();
      PactBrokerClient pactBrokerClient = newPactBrokerClient(uriBuilder.build());
      List<PactBrokerConsumer> pactBrokerConsumers;
      if (StringUtils.isEmpty(tag)) {
        pactBrokerConsumers = pactBrokerClient.fetchConsumers(providerName);
      } else {
        pactBrokerConsumers = pactBrokerClient.fetchConsumersWithTag(providerName, tag);
      }

      for (PactBrokerConsumer consumer: pactBrokerConsumers) {
        consumers.add(ConsumerInfo.from(consumer));
      }

      if (failIfNoPactsFound && consumers.isEmpty()) {
        throw new NoPactsFoundException("No consumer pacts were found for provider '" + providerName + "' and tag '" +
          tag + "'. (URL " + pactBrokerClient.getUrlForProvider(providerName, tag) + ")");
      }

      List<Pact> pacts = new ArrayList<>();
      for (ConsumerInfo consumer: consumers) {
        pacts.add(this.loadPact(consumer, pactBrokerClient.getOptions()));
      }
      return pacts;
    } catch (URISyntaxException e) {
      throw new IOException("Was not able load pacts from broker as the broker URL was invalid", e);
    }
  }

  Pact loadPact(ConsumerInfo consumer, Map options) {
    Pact pact = PactReader.loadPact(options, consumer.getPactSource());
    Map<Consumer, List<Pact>> pacts = this.pactSource.getPacts();
    Consumer pactConsumer = consumer.toPactConsumer();
    List<Pact> pactList = pacts.get(pactConsumer);
    if (pactList == null) {
      pactList = new ArrayList<>();
      pacts.put(pactConsumer, pactList);
    }
    pactList.add(pact);
    pacts.put(pactConsumer, pactList);
    return pact;
  }

  PactBrokerClient newPactBrokerClient(URI url) throws URISyntaxException {
    HashMap options = new HashMap();
    if (this.authentication != null && !this.authentication.scheme().equalsIgnoreCase("none")) {
      options.put("authentication", Arrays.asList(parseExpression(this.authentication.scheme()),
        parseExpression(this.authentication.username()), parseExpression(this.authentication.password())));
    }
    return new PactBrokerClient(url.toString(), options);
  }

  public boolean isFailIfNoPactsFound() {
    return failIfNoPactsFound;
  }

  public void setFailIfNoPactsFound(boolean failIfNoPactsFound) {
    this.failIfNoPactsFound = failIfNoPactsFound;
  }
}
