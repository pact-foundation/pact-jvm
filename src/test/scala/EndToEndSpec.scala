import com.dius.pact.runner.Server
import org.specs2.mutable.Specification

class EndToEndSpec extends Specification {

  "PactRunner" should {
    "Run Pacts" in {
      Server.start()(scala.concurrent.ExecutionContext.Implicits.global)

      val basePath = "src/test/resources/"

      val testJson = s"$basePath/pacts"

      val testConfig = s"$basePath/pact-config.json"

      Main.main(Array(testJson, testConfig))

      Server.stop()(scala.concurrent.ExecutionContext.Implicits.global)

      true must beTrue
    }
  }

}
