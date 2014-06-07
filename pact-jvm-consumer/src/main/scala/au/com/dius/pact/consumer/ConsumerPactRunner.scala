package au.com.dius.pact.consumer

import au.com.dius.pact.model.Pact
import scala.util.Try
import scala.util.Success

object ConsumerPactRunner {
  
  def writeIfMatching(pact: Pact, results: PactSessionResults): VerificationResult = 
    writeIfMatching(pact, Success(results))
  
  def writeIfMatching(pact: Pact, tryResults: Try[PactSessionResults]): VerificationResult = {
    for (results <- tryResults if results.allMatched) {
      PactGenerator.merge(pact).writeAllToFile()
    }
    VerificationResult(tryResults)
  }
  
  def runAndWritePact[T](pact: Pact)(userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val server = DefaultMockProvider.withDefaultConfig()
    new ConsumerPactRunner(server).runAndWritePact(pact)(userCode, userVerification)
  }
}

class ConsumerPactRunner(server: MockProvider) {
  import ConsumerPactRunner._
  
  def runAndWritePact[T](pact: Pact)(userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val tryResults = server.runAndClose(pact)(userCode, userVerification)
    writeIfMatching(pact, tryResults)
  }
  
  def runAndWritePact(pact: Pact, userCode: Runnable): VerificationResult =
    runAndWritePact(pact)(userCode.run(), (u:Unit) => Success(u))
  
}