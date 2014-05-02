package au.com.dius.pact.consumer

import java.io.File
import java.io.PrintWriter
import scalaz.@@

import scala.util.Try

import au.com.dius.pact.model.Pact

object PactGenerator {
  //TODO: use environment property for pact output folder
  val pactRootDir = "target/pacts"
}

sealed trait Filename

case class PactGenerator(pact: Pact) {
  import PactGenerator._
  
  val defaultFilename: String = 
    s"${pact.consumer.name}-${pact.provider.name}.json"

  def pactDestination = s"$pactRootDir/$defaultFilename"
  
  def createPactRootDir(): Unit = new File(pactRootDir).mkdirs()

  def writeToFile(): Try[Unit] = Try {
    createPactRootDir()
    val writer = new PrintWriter(new File(pactDestination))
    try {
      pact.sortInteractions.serialize(writer)
    } finally {
      writer.close()
    }
  }
}
