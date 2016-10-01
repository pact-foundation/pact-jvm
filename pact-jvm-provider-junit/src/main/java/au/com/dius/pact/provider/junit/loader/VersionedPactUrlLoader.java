package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.Pact;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Implementation of {@link PactLoader} that downloads pacts from given urls containing versions to be filtered in from system properties.
 *
 * @see VersionedPactUrl usage instructions
 */
public class VersionedPactUrlLoader implements PactLoader {
    private final String[] urls;

    public VersionedPactUrlLoader(String[] urls) {
        this.urls = urls;
    }

    @SuppressWarnings("unused")
    public VersionedPactUrlLoader(VersionedPactUrl pactUrl) {
        this(pactUrl.urls());
    }

    @Override
    public List<Pact> load(String providerName) throws IOException {
        return new PactUrlLoader(expandVariables(urls)).load(providerName);
    }

    @VisibleForTesting
    static String[] expandVariables(String[] urls) throws IOException {
        List<String> list = new ArrayList<String>();
        for (String url: urls) {
          list.add(expandVariables(url));
        }
        return list.toArray(new String[urls.length]);
    }

    private static String expandVariables(String urlWithVariables) {
        String urlWithVersions = urlWithVariables;
        for (Map.Entry<Object, Object> property : System.getProperties().entrySet()) {
            urlWithVersions = urlWithVersions.replace(format("${%s}", property.getKey()), property.getValue().toString());
        }
        if (urlWithVersions.matches(".*\\$\\{[a-z\\.]+\\}.*")) {
            throw new IllegalArgumentException(urlWithVersions + " contains variables that could not be any of the system properties. Define a system property to replace them or remove the variables from the URL.");
        }
        return urlWithVersions;
    }
}
