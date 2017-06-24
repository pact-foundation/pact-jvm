package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.*;
import au.com.dius.pact.model.PactSource;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link PactLoader} that downloads pacts from given urls
 */
public class PactUrlLoader implements PactLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PactUrlLoader.class);
    private final String[] urls;
    private final UrlsSource pactSource;

  public PactUrlLoader(final String[] urls) {
    this.urls = urls;
    this.pactSource = new UrlsSource(Arrays.stream(urls).collect(Collectors.toList()));
  }

  public PactUrlLoader(final PactUrl pactUrl) {
        this(pactUrl.urls());
    }

  public List<Pact> load(final String providerName) throws IOException {
      return Arrays.stream(urls)
        .map(url -> {
          Pact pact = PactReader.loadPact(url);
          this.getPactSource().getPacts().put(url, pact);
          return pact;
        })
        .collect(toList());
  }

  @Override
  public UrlsSource getPactSource() {
    return pactSource;
  }
}
