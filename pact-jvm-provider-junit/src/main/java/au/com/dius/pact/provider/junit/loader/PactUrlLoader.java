package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link PactLoader} that downloads pacts from given urls
 */
public class PactUrlLoader implements PactLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PactUrlLoader.class);
    private final String[] urls;

    public PactUrlLoader(final String[] urls) {
        this.urls = urls;
    }

    public PactUrlLoader(final PactUrl pactUrl) {
        this(pactUrl.urls());
    }

    public List<Pact> load(final String providerName) throws IOException {
      List<Pact> pacts = new ArrayList<Pact>();
      for (String url: urls) {
        pacts.add(PactReader.loadPact(url));
      }
      return pacts;
    }
}
