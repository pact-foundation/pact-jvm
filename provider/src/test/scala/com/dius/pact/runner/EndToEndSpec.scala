package com.dius.pact.runner

import akka.actor.ActorSystem
import java.net.URL
import org.specs2.mutable.Specification
import scala.concurrent.duration.Duration

class EndToEndSpec extends Specification {

  def loadResource(name: String): URL = {
    this.getClass.getClassLoader.getResource(name)
  }

  "PactRunner" should {
    "Run Pacts" in {
      val testJson = loadResource(s"pacts").getPath
      val testConfig = loadResource(s"pact-config.json").getPath

      val system = ActorSystem("Test-Provider-System")
      implicit val executionContext = system.dispatcher
      val server = Server(system)
      val future = for {
        //TODO: externalise interface and port
        started <- server.start()
        _ = Main.runStuff(Array(testJson, testConfig))
        stopped <- started.stop()
        _ = system.shutdown()
      } yield { stopped }

      //TODO: externalise timeout configuration
      future must beEqualTo(server).await(timeout = Duration(10, "s"))
    }
  }
}
