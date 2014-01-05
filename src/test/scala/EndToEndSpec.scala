import akka.actor.ActorSystem
import com.dius.pact.runner.Server
import org.specs2.mutable.Specification
import scala.concurrent.duration.Duration

class EndToEndSpec extends Specification {

  "PactRunner" should {
    "Run Pacts" in {
      val basePath = "src/test/resources/"
      val testJson = s"$basePath/pacts"
      val testConfig = s"$basePath/pact-config.json"

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
