package au.com.dius.pact.provider

import akka.actor.ActorSystem
import java.net.URL
import org.specs2.mutable.Specification
import scala.concurrent.duration.Duration
import java.io.File

class MainSpec extends Specification {

  def loadResource(name: String): URL = {
    this.getClass.getClassLoader.getResource(name)
  }

  "PactRunner" should {
    "Run Pacts" in {
      val testJson = new File(loadResource(s"pacts").getPath)
      val testConfig = new File(loadResource(s"pact-config.json").getPath)

      val system = ActorSystem("Test-Provider-System")
      try {
        implicit val executionContext = system.dispatcher
        val server = Server(system)
        val future = for {
          started <- server.start()
          _ = Main.runPacts(Main.loadFiles(testJson, testConfig))(system)
          stopped <- started.stop()
        } yield { stopped }

        future must beEqualTo(server).await(timeout = Duration(10, "s"))
      } finally {
        system.shutdown()
      }
    }
  }
}
