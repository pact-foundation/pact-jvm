package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactReader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Out-of-the-box implementation of {@link PactLoader}
 * that loads pacts from subfolder of project resource folder
 */
public class PactFolderLoader implements PactLoader {
    private final File path;

    public PactFolderLoader(final File path) {
        this.path = path;
    }

    public PactFolderLoader(final String path) {
        this(new File(path));
    }

    public PactFolderLoader(final URL path) {
        this(path == null ? "" : path.getPath());
    }

    public PactFolderLoader(final PactFolder pactFolder) {
        this(PactFolderLoader.class.getClassLoader().getResource(pactFolder.value()));
    }

    @Override
    public List<Pact> load(final String providerName) throws IOException {
        final PactReader pactReader = new PactReader();
        return Arrays.stream(path.listFiles((dir, name) -> name.endsWith(".json")))
                .map(pactReader::loadPact)
                .map(object -> (Pact) object)
                .filter(pact -> pact.provider().name().equals(providerName))
                .collect(toList());
    }
}
