package au.com.dius.pact.provider

import au.com.dius.pact.model.Pact
import org.scalatest._
import org.json4s.jackson.JsonMethods._
import java.io.File

object Main {

  def loadFiles(pactRoot: File, configFile: File) = {
    implicit val formats = org.json4s.DefaultFormats
    val config = parse(scala.io.Source.fromFile(configFile).mkString).extract[PactConfiguration]
    (config, PactFileSource.loadFiles(pactRoot))
  }

  def runPacts(t:(PactConfiguration, Seq[Pact])) = t match { case (config, pacts) =>
    val suite = new Sequential(pacts.map { pact =>
      new PactSpec(config, pact)
    }: _*)
    stats.fullstacks.run(suite)
  }
}
