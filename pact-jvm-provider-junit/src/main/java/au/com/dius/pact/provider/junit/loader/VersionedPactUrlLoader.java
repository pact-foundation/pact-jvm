package au.com.dius.pact.provider.junit.loader;

import com.google.common.annotations.VisibleForTesting;

import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.stream;

/**
 * Implementation of {@link PactLoader} that downloads pacts from given urls containing versions to be filtered in from system properties.
 *
 * @see VersionedPactUrl usage instructions
 */
public class VersionedPactUrlLoader extends PactUrlLoader {

    public VersionedPactUrlLoader(String[] urls) {
      super(expandVariables(urls));
    }

    @SuppressWarnings("unused")
    public VersionedPactUrlLoader(VersionedPactUrl pactUrl) {
        this(pactUrl.urls());
    }

    @VisibleForTesting
    static String[] expandVariables(String[] urls) {
        return stream(urls)
                .map(VersionedPactUrlLoader::expandVariables)
                .collect(Collectors.toList())
                .toArray(new String[urls.length]);
    }

    private static String expandVariables(String urlWithVariables) {
        String urlWithVersions = urlWithVariables;
        if (!variablesToExpandFound(urlWithVersions)) {
            throw new IllegalArgumentException(urlWithVersions + " contains no variables to expand in the format ${...}. Consider using @PactUrl or providing expandable variables.");
        }
        for (Map.Entry<Object, Object> property : System.getProperties().entrySet()) {
            urlWithVersions = urlWithVersions.replace(format("${%s}", property.getKey()), property.getValue().toString());
        }
        if (variablesToExpandFound(urlWithVersions)) {
            throw new IllegalArgumentException(urlWithVersions + " contains variables that could not be any of the system properties. Define a system property to replace them or remove the variables from the URL.");
        }
        return urlWithVersions;
    }

    private static boolean variablesToExpandFound(String urlWithVersions) {
        return urlWithVersions.matches(".*\\$\\{[a-z\\.]+\\}.*");
    }
}
