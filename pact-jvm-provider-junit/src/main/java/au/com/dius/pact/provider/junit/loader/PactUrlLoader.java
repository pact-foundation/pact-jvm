package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactReader;
import au.com.dius.pact.model.UrlsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
    this.pactSource = new UrlsSource(Arrays.asList(urls));
  }

  public PactUrlLoader(final PactUrl pactUrl) {
        this(pactUrl.urls());
    }

  public List<Pact> load(final String providerName) {
      List<Pact> pacts = new ArrayList<>();
      for(String url: urls) {
        Pact pact = PactReader.loadPact(url);
        this.getPactSource().getPacts().put(url, pact);
        pacts.add(pact);
      }
      return pacts;
  }

  @Override
  public UrlsSource getPactSource() {
    return pactSource;
  }
}
