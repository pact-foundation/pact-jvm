package au.com.dius.pact.provider.scalasupport

import java.io.File

import au.com.dius.pact.core.model.RequestResponsePact
import org.scalatest._

object Main {

  def loadFiles(pactRoot: File, configFile: File) = {
    val config = PactConfiguration.loadConfiguration(configFile)
    (config, PactFileSource.loadFiles(pactRoot))
  }

  def runPacts(t:(PactConfiguration, Seq[RequestResponsePact])) = t match { case (config, pacts) =>
    val suite = new Sequential(pacts.map { pact =>
      new PactSpec(config, pact)
    }: _*)
    stats.fullstacks.run(suite)
  }
}
