import com.dius.pact.model.Pact
import com.dius.pact.runner.{PactConfiguration, PactSpec, PactFileSource}
import org.scalatest._
import scala.concurrent.ExecutionContext
import org.json4s._
import org.json4s.jackson.JsonMethods._

object Main {
  def main(args: Array[String]) {
    runStuff(args)
  }

  //decoupled from command line runner so specs can provide execution context
  def runStuff(args: Array[String])(implicit executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global) = {
    args.headOption.fold (println("Need pact root as first arg")) {dir =>
      args.tail.headOption.fold (println("need config file as second arg")) { config =>
        loadFiles(dir, config)
      }
    }
  }

  def loadFiles(dir: String, configFile: String)(implicit executionContext: ExecutionContext) = {
    implicit val formats = org.json4s.DefaultFormats
    val map = parse(io.Source.fromFile(configFile).mkString).extract[Map[String,String]]
    val config = PactConfiguration(map)
    try {
      runPacts(config, PactFileSource.loadFiles(dir))
    } catch {
      case t: Throwable => t.printStackTrace()
    }
  }

  def runPacts(config:PactConfiguration, pacts:Seq[Pact])(implicit executionContext: ExecutionContext) = {
    stats.run (
      new Sequential(pacts.map { pact =>
        new PactSpec(config, pact)
      } :_*)
    )
  }
}