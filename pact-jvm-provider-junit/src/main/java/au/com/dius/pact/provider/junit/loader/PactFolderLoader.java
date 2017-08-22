package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.DirectorySource;
import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactReader;
import au.com.dius.pact.model.PactSource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Out-of-the-box implementation of {@link PactLoader}
 * that loads pacts from either a subfolder of project resource folder or a directory
 */
public class PactFolderLoader implements PactLoader {
    private final File path;
    private DirectorySource pactSource;

    public PactFolderLoader(final File path) {
      this.path = path;
      this.pactSource = new DirectorySource(path);
    }

    public PactFolderLoader(final String path) {
      this(new File(path));
    }

    /**
     * @deprecated Use PactUrlLoader for URLs
     */
    @Deprecated
    public PactFolderLoader(final URL path) {
        this(path == null ? "" : path.getPath());
    }

    public PactFolderLoader(final PactFolder pactFolder) {
        this(pactFolder.value());
    }

    @Override
    public List<Pact> load(final String providerName) {
        List<Pact> pacts = new ArrayList<Pact>();
        File pactFolder = resolvePath();
        File[] files = pactFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                Pact pact = PactReader.loadPact(file);
                if (pact.getProvider().getName().equals(providerName)) {
                  pacts.add(pact);
                  this.pactSource.getPacts().put(file, pact);
                }
            }
        }
        return pacts;
    }

    @Override
    public PactSource getPactSource() {
        return this.pactSource;
    }

    public Map<Pact, File> loadPactsWithFiles(final String providerName) throws IOException {
      return this.pactSource.getPacts().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

  private File resolvePath() {
    File file = path;
    URL resourcePath = PactFolderLoader.class.getClassLoader().getResource(path.getPath());
    if (resourcePath != null) {
      file = new File(resourcePath.getPath());
    }
    return file;
  }
}
