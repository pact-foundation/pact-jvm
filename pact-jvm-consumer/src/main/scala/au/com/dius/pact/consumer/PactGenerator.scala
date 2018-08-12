package au.com.dius.pact.consumer

import java.io.{File, PrintWriter}

import au.com.dius.pact.core.model.{Pact => PactModel, _}
import com.typesafe.scalalogging.StrictLogging

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

  def defaultFilename[I <: Interaction](pact: PactModel[I]): String = s"${pact.getConsumer.getName}-${pact.getProvider.getName}.json"

  def destinationFileForPact[I <: Interaction](pact: PactModel[I]): File = destinationFile(defaultFilename(pact))
  def destinationFile(filename: String): File = new File(s"${PactConsumerConfig.pactRootDir}/$filename")
  
  def merge(pact: PactModel[RequestResponseInteraction]): PactGenerator = synchronized {
    pactGen = pactGen merge pact
    pactGen
  }
  
  private var pactGen = new PactGenerator(Map(), Nil)
    
}

case class PactGenerator(pacts: Map[String, PactModel[RequestResponseInteraction]], conflicts: List[String]) extends StrictLogging {
  import PactGenerator._
  
  def failed: Boolean = conflicts.nonEmpty
  
  def isEmpty: Boolean = pacts.isEmpty

  def merge[I <: Interaction](pact: PactModel[RequestResponseInteraction]): PactGenerator = {
    val pactFileName = defaultFilename(pact)
    val existingPact = pacts get pactFileName
    def directlyAddPact(p: PactModel[RequestResponseInteraction]) =
      PactGenerator(pacts + (pactFileName -> p), conflicts)

    existingPact.fold(directlyAddPact(pact)) { existing =>
      val result = PactMerge.merge[RequestResponseInteraction](pact, existing)
      if (result.getOk) {
        directlyAddPact(result.getResult)
      } else {
        PactGenerator(pacts, result.getMessage :: conflicts)
      }
    }
  }

  def writeAllToFile(pactVersion: PactSpecVersion): Unit = {
    def createPactRootDir(): Unit = 
      new File(PactConsumerConfig.pactRootDir).mkdirs()
    
    def writeToFile[I <: Interaction](pact: PactModel[I], filename: String): Unit = {
      val file = destinationFileForPact(pact)
      logger.debug(s"Writing pact ${pact.getConsumer.getName} -> ${pact.getProvider.getName} to file $file")
      val writer = new PrintWriter(file)
      try PactWriter.writePact[I](pact, writer, pactVersion)
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
