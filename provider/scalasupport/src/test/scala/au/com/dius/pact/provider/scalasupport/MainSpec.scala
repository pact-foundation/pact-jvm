package au.com.dius.pact.provider.scalasupport

import java.io.File
import java.net.URL

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{AfterAll, BeforeAll}
import unfiltered.netty.Server

trait TestServerContext extends BeforeAll with AfterAll {
  var server: Server

  override def beforeAll() = {
    server = TestService(8888)
  }

  override def afterAll() = {
    server.stop()
  }
}

@RunWith(classOf[JUnitRunner])
class MainSpec extends Specification with TestServerContext {

  def loadResource(name: String): URL = {
    this.getClass.getClassLoader.getResource(name)
  }

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

  override var server: Server = _
}
