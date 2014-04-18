package au.com.dius.pact.provider

import java.net.URL
import org.specs2.mutable.Specification
import java.io.File

class MainSpec extends Specification {

  def loadResource(name: String): URL = {
    this.getClass.getClassLoader.getResource(name)
  }

  "PactRunner" should {
    "Run Pacts" in {
      val testJson = new File(loadResource(s"pacts").getPath)
      val testConfig = new File(loadResource(s"pact-config.json").getPath)
      val server = TestService(8888)
      try {
        Main.runPacts(Main.loadFiles(testJson, testConfig))
      } finally {
        server.stop()
      }
      success
    }
  }
}
