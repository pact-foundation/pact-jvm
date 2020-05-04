package au.com.dius.pact.provider.junit.loader

import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junitsupport.loader.PactFolderLoader
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is

@PactFolder('pacts')
class PactFolderLoaderTest {

  @Test
  void 'handles the case where the configured directory does not exist'() {
    assertThat(new PactFolderLoader(new File('/does/not/exist')).load('provider'), is(empty()))
  }

  @Test
  void 'only includes json files'() {
    assertThat(new PactFolderLoader(this.class.getAnnotation(PactFolder)).load('myAwesomeService'), hasSize(3))
  }

  @Test
  void 'only includes json files that match the provider name'() {
    assertThat(new PactFolderLoader(this.class.getAnnotation(PactFolder)).load('myAwesomeService2'), hasSize(1))
  }

  @Test
  void 'is able to load files from a directory'() {
    File tmpDir = File.createTempDir()
    tmpDir.deleteOnExit()
    File pactFile = new File(tmpDir, 'pact.json')
    pactFile.deleteOnExit()
    pactFile.text = this.class.classLoader.getResourceAsStream('pacts/contract.json').text

    assertThat(new PactFolderLoader(tmpDir.path).load('myAwesomeService'), hasSize(1))
  }

  @Test
  void 'is able to load files from a directory with spaces in the path'() {
    assert new PactFolderLoader('dir with spaces!').load('myAwesomeService').size() == 1
  }

}
