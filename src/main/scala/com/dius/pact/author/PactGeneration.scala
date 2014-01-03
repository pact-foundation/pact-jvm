package com.dius.pact.author

import com.dius.pact.consumer.PactVerification.{PactVerified, VerificationResult}
import com.dius.pact.model.Pact
import java.io.{PrintWriter, File}

object PactGeneration {
  //TODO: handle multiple sources writing interactions to the same pact, threadsafe merge and verify
  def apply(pact: Pact, verification: VerificationResult) {
    verification match {
      case PactVerified => {
        writeToDisk(pact)
      }
      case _ => println("pact not verified, skipping file output")
    }
  }

  def writeToDisk(pact: Pact) {
    //TODO: use environment property for pact output folder
    new File("target/pacts").mkdirs()
    val writer = new PrintWriter(new File(s"target/pacts/${pact.consumer.name}-${pact.provider.name}.json"))
    try {
      pact.serialize(writer)
    } finally {
      writer.close()
    }
  }
}
