package com.dius.pact.runner

import com.dius.pact.model.Pact
import org.scalatest._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import java.io.File
import akka.actor.ActorSystem

object Main {
  def loadFiles(pactRoot: File, configFile: File) = {
    implicit val formats = org.json4s.DefaultFormats
    val config = parse(io.Source.fromFile(configFile).mkString).extract[PactConfiguration]
    (config, PactFileSource.loadFiles(pactRoot))
  }

  def runPacts(t:(PactConfiguration, Seq[Pact]))(implicit actorSystem: ActorSystem) = t match { case (config, pacts) =>
    stats.fullstacks.run (
      new Sequential(pacts.map { pact =>
        new PactSpec(config, pact)
      } :_*)
    )
  }
}