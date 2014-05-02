package au.com.dius.pact.consumer

import au.com.dius.pact.model.Pact


case class ConsumerPactRunner(server: PactServer) {
  
  def runAndWritePact(pact: Pact)(userCode: => Unit): VerificationResult = {
    val tryResults = server.runAndClose(pact)(userCode)
    for (results <- tryResults if results.allMatched) {
      PactGenerator(pact).writeToFile()
    }
    VerificationResult(tryResults)
  }
  
  def runAndWritePact(pact: Pact, test: Runnable): VerificationResult = 
    runAndWritePact(pact)(test.run())
}