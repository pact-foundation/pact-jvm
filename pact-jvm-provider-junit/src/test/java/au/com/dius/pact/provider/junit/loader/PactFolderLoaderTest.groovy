package au.com.dius.pact.provider.junit.loader

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.is

class PactFolderLoaderTest {

  @Test
  void 'handles the case where the configured directory does not exist'() {
    assertThat(new PactFolderLoader(new File('/does/not/exist')).load('provider'), is(empty()))
  }

}
