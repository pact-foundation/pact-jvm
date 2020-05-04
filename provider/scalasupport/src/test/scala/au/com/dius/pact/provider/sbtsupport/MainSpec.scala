package au.com.dius.pact.provider.sbtsupport

import java.io.File
import java.net.URL

import org.junit.Ignore
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import unfiltered.netty.Server

//@RunWith(classOf[JUnitRunner])
@Ignore
class MainSpec extends Specification {

  def loadResource(name: String): URL = {
    this.getClass.getClassLoader.getResource(name)
  }

  var server: Server = null
  step(server = TestService(8888))

  "PactRunner" should {

    "Run Pacts" in {
      val testJson = new File(loadResource("pacts").getPath)
      val testConfig = new File(loadResource("pact-config.json").getPath)
      Main.runPacts(Main.loadFiles(testJson, testConfig))
      success
    }

    "Fail in a meaningful way" in {
      val testJson = new File(loadResource("failingPacts").getPath)
      val testConfig = new File(loadResource("pact-config.json").getPath)
      Main.runPacts(Main.loadFiles(testJson, testConfig))
      success
    }

  }

  step(server.stop())

}
