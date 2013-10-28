import com.dius.pact.model.Pact
import com.dius.pact.runner.http.Client
import com.dius.pact.runner.{PactConfiguration, PactRunner, PactFileSource}
import play.api.libs.json.{JsError, Json}

import scala.concurrent.ExecutionContext.Implicits.global

object Main {
  def main(args: Array[String]) = {
    args.headOption.fold (println("Need pact root as first arg")) {dir =>
      args.tail.headOption.fold (println("need config file as second arg")) { config =>
        loadFiles(dir, config)
      }
    }
  }

  def loadFiles(dir:String, config:String) = {
    Json.parse(io.Source.fromFile(config).mkString).validate[Map[String,String]].map { mapping =>
      runPacts(PactConfiguration(mapping, new Client), PactFileSource.loadFiles(dir))
    }.recover {
      case e => println(s"Incorrect pact config file: ${Json.stringify(JsError.toFlatJson(e))}")
    }
  }

  def runPacts(config:PactConfiguration, pacts:Seq[Pact]) = {
    val runner = PactRunner(config)

    pacts.map(runner.run)
  }
}