package au.com.dius.pact.provider

import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.configuration.PactConfiguration
import org.scalatest._
import java.io.File

object Main {

  def loadFiles(pactRoot: File, configFile: File) = {
    val config = PactConfiguration.loadConfiguration(configFile)
    (config, PactFileSource.loadFiles(pactRoot))
  }

  def runPacts(t:(PactConfiguration, Seq[Pact])) = t match { case (config, pacts) =>
    val suite = new Sequential(pacts.map { pact =>
      new PactSpec(config, pact)
    }: _*)
    stats.fullstacks.run(suite)
  }
}
