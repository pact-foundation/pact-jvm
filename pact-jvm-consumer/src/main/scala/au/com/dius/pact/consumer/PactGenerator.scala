package au.com.dius.pact.consumer

import java.io.{PrintWriter, File}

import com.typesafe.scalalogging.StrictLogging
import au.com.dius.pact.model._

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

  def defaultFilename(pact: Pact): String = s"${pact.getConsumer.getName}-${pact.getProvider.getName}.json"

  def destinationFileForPact(pact: Pact): File = destinationFile(defaultFilename(pact))
  def destinationFile(filename: String): File = new File(s"${PactConsumerConfig.pactRootDir}/$filename")
  
  def merge(pact: Pact): PactGenerator = synchronized {
    pactGen = pactGen merge pact
    pactGen
  }
  
  private var pactGen = new PactGenerator(Map(), Nil)
    
}

case class PactGenerator(pacts: Map[String, Pact], conflicts: List[MergeConflict]) extends StrictLogging {
  import PactGenerator._
  
  def failed: Boolean = conflicts.nonEmpty
  
  def isEmpty: Boolean = pacts.isEmpty
  
  def merge(pact: Pact): PactGenerator = {
    val pactFileName = defaultFilename(pact)
    val existingPact = pacts get pactFileName
    def directlyAddPact(p: Pact) = 
      PactGenerator(pacts + (pactFileName -> p), conflicts)

    existingPact.fold(directlyAddPact(pact)) { existing =>
      PactMerge.merge(pact, existing) match {
        case MergeSuccess(merged) => directlyAddPact(merged)
        case c @ MergeConflict(_) => PactGenerator(pacts, c :: conflicts)
      }
    } 
  }

  def writeAllToFile(pactVersion: PactSpecVersion): Unit = {
    def createPactRootDir(): Unit = 
      new File(PactConsumerConfig.pactRootDir).mkdirs()
    
    def writeToFile(pact: Pact, filename: String): Unit = {
      val file = destinationFileForPact(pact)
      logger.debug(s"Writing pact ${pact.getConsumer.getName} -> ${pact.getProvider.getName} to file $file")
      val writer = new PrintWriter(file)
      try PactWriter.writePact(pact, writer, pactVersion)
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
