package au.com.dius.pact.consumer

import java.io.File
import java.io.PrintWriter

import scala.util.{Try, Success, Failure}

import au.com.dius.pact.model.Pact
import Pact.{MergeSuccess, MergeConflict}
import PactGenerator._

/**
 * Globally accumulates Pacts, merges by destination file, and allows writing to File.
 * 
 * This must be mutable, since there is otherwise no way to thread the state through 
 * whatever testing framework is in use.
 * 
 * Ideally writing would happen only at the end of the full test suite, but it may be necessary
 * to write each time, and synchronise on disk, such that the file read and write can not be done concurrently
 * with another running test.
 * 
 * This code has a way to go before it is fit for purpose.
 */
object PactGenerator {
  //TODO: use environment property for pact output folder
  val pactRootDir = "target/pacts"
    
  def defaultFilename(pact: Pact): String = 
    s"${pact.consumer.name}-${pact.provider.name}.json"

  def destinationFileForPact(pact: Pact): File = destinationFile(defaultFilename(pact))
  def destinationFile(filename: String): File = new File(s"$pactRootDir/$filename")
  
  def merge(pact: Pact): PactGenerator = synchronized {
    pactGen = pactGen merge pact
    pactGen
  }
  
  private var pactGen = new PactGenerator(Map(), Nil)
    
}

case class PactGenerator(pacts: Map[String, Pact], conflicts: List[MergeConflict]) {
  import PactGenerator._
  
  def failed: Boolean = conflicts.nonEmpty
  
  def isEmpty: Boolean = pacts.isEmpty
  
  def merge(pact: Pact): PactGenerator = {
    val pactFileName = defaultFilename(pact)
    val existingPact = pacts get pactFileName
    def directlyAddPact(p: Pact) = 
      PactGenerator(pacts + (pactFileName -> p), conflicts)

    existingPact.fold(directlyAddPact(pact)) { existing => 
      pact.merge(existing) match {
        case MergeSuccess(merged) => directlyAddPact(merged)
        case c @ MergeConflict(_) => PactGenerator(pacts, c :: conflicts)
      }
    } 
  }

  def writeAllToFile(): Unit = {
    def createPactRootDir(): Unit = 
      new File(pactRootDir).mkdirs()
    
    def writeToFile(pact: Pact, filename: String): Unit = {
      val file = destinationFileForPact(pact)
      val writer = new PrintWriter(file)
      try pact.sortInteractions.serialize(writer)
      finally writer.close()
    }
    require(!isEmpty, "Cannot write to file; no pacts have been recorded")
    require(!failed, "The following merge conflicts occurred: \n" + conflicts.mkString("\n - "))
    
    createPactRootDir()
    
    pacts foreach { 
      case (filename, pact) => writeToFile(pact, filename)
    }
  }
}
