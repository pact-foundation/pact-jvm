import com.dius.pact.model.Pact
import com.dius.pact.runner.http.Client
import com.dius.pact.runner.{PactConfiguration, PactSpec, PactFileSource}
import org.scalatest.Sequential
import play.api.libs.json.{JsError, Json}
import scala.concurrent.ExecutionContext

object Main {
  def main(args: Array[String]) = {
    run(args)
  }

  //decoupled from command line runner so specs can provide execution context
  def run(args: Array[String])(implicit executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global) = {
    args.headOption.fold (println("Need pact root as first arg")) {dir =>
      args.tail.headOption.fold (println("need config file as second arg")) { config =>
        loadFiles(dir, config)
      }
    }
  }

  def loadFiles(dir:String, config:String)(implicit executionContext: ExecutionContext) = {
    Json.parse(io.Source.fromFile(config).mkString).validate[Map[String,String]].map { mapping =>
      try {
        runPacts(PactConfiguration(mapping, new Client), PactFileSource.loadFiles(dir))
        println("ran em")
      } catch {
        case t: Throwable => t.printStackTrace()
      }
    }.recover {
      case e => println(s"Incorrect pact config file: ${Json.stringify(JsError.toFlatJson(e))}")
    }
  }

  def runPacts(config:PactConfiguration, pacts:Seq[Pact])(implicit executionContext: ExecutionContext) = {
    org.scalatest.run(
      new Sequential(pacts.map { pact =>
        new PactSpec(config, pact)
      } :_*)
    )
  }
}