package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.DirectorySource;
import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactReader;
import au.com.dius.pact.model.PactSource;
import au.com.dius.pact.provider.junit.loader.PactLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GitPactLoader implements PactLoader {
  private final File path;
  private DirectorySource pactSource;

  public GitPactLoader(final Class<?> testClass) throws MalformedURLException {
    final Git git = testClass.getAnnotation(Git.class);
    if (git != null) {
        URL gitUrl = new URL(git.value());
        // Get content
        String folder = gitUrl.getPath();
        final URL resource = GitPactLoader.class.getClassLoader().getResource(folder.substring(1, folder.length()));
        this.path = new File(resource.getPath());
        this.pactSource = new DirectorySource(this.path);
    } else {
        throw new IllegalArgumentException("One Git annotation must be provided.");
    }
  }

  @Override
  public List<Pact> load(final String providerName) {
    List<Pact> pacts = new ArrayList<Pact>();
    File[] files = path.listFiles((dir, name) -> name.endsWith(".json"));
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
    return pactSource;
  }
}
