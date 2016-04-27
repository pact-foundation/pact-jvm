package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactReader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Out-of-the-box implementation of {@link PactLoader}
 * that loads pacts from either a subfolder of project resource folder or a directory
 */
public class PactFolderLoader implements PactLoader {
    private final String path;

    public PactFolderLoader(final File path) {
        this(path.getPath());
    }

    public PactFolderLoader(final String path) {
      this.path = path;
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
    public List<Pact> load(final String providerName) throws IOException {
        List<Pact> pacts = new ArrayList<Pact>();
        File pactFolder = resolvePath();
        File[] files = pactFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".json");
            }
        });
        if (files != null) {
            for (File file : files) {
                Pact pact = PactReader.loadPact(file);
                if (pact.getProvider().getName().equals(providerName)) {
                    pacts.add(pact);
                }
            }
        }
        return pacts;
    }

  private File resolvePath() {
    File file = new File(path);
    URL resourcePath = PactFolderLoader.class.getClassLoader().getResource(path);
    if (resourcePath != null) {
      file = new File(resourcePath.getPath());
    }
    return file;
  }
}
