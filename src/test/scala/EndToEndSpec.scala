import com.dius.pact.runner.http.Server
import java.io.File
import org.specs2.mutable.Specification

class EndToEndSpec extends Specification {

  "PactRunner" should {
    "Run Pacts" in {
      Server.start()

      val basePath = "src/test/resources/"

      val testJson = s"$basePath/pacts"

      val testConfig = s"$basePath/pact-config.json"

      Main.main(Array(testJson, testConfig))

      Server.stop()

      true must beTrue
    }
  }

}
