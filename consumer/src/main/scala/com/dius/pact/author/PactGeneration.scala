package com.dius.pact.author

import com.dius.pact.consumer.PactVerification.{PactMergeFailed, PactWritten, PactVerified, VerificationResult}
import com.dius.pact.model.{Interaction, Pact}
import java.io.{PrintWriter, File}
import scala.concurrent.Future
import com.dius.pact.model.Pact.{ConflictingInteractions, MergeSuccess}

object PactGeneration {
  private val hackyPactStore = scala.collection.mutable.Map[String, Pact]()

  //TODO: handle multiple sources writing interactions to the same pact, threadsafe merge and verify
  def apply(pact: Pact, verification: VerificationResult): VerificationResult = {
    verification match {
      case PactVerified => {
        hackyPactStore.synchronized {
          merge(pact)
        }
      }
      case _ => verification
    }
  }

  def merge(pact: Pact): VerificationResult = {
    val pactFileName = s"${pact.consumer.name}-${pact.provider.name}.json"
    val existingPact = hackyPactStore.getOrElse(pactFileName, Pact(pact.provider, pact.consumer, Seq()))

    Pact.merge(pact, existingPact) match {
      case MergeSuccess(merged) => {
        hackyPactStore.put(pactFileName, merged)
        writeToFile(pactFileName, merged)
      }
      case failed:ConflictingInteractions => {
        PactMergeFailed(failed)
      }
    }
  }

  /**
   * sort keys in the pact so that serialization is consistent
   *
   * @param pact
   * @return
   */
  def sort(pact: Pact): Pact = {
    pact.copy(interactions = pact.interactions.sortBy{i => i.providerState+ i.description})
  }

  def writeToFile(fileName: String, pact: Pact): VerificationResult = {
    //TODO: use environment property for pact output folder
    val pactRootDir = "target/pacts"
    val pactDestination = s"$pactRootDir/$fileName"
    try {
      new File(pactRootDir).mkdirs()
      val writer = new PrintWriter(new File(pactDestination))
      sort(pact).serialize(writer)
      writer.close()
      println(s"pact written to: $pactDestination")
      PactVerified
    } catch {
      case t:Throwable => {
        println(s"unable to write: $pact")
        throw t
      }
  }
  }
}
