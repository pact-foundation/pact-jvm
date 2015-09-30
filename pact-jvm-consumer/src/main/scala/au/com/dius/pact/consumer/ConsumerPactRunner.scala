package au.com.dius.pact.consumer

import au.com.dius.pact.model.{PactSpecVersion, PactConfig, Pact}
import scala.util.{Failure, Try, Success}

object ConsumerPactRunner {
  
  def writeIfMatching(pact: Pact, results: PactSessionResults, config: PactConfig): VerificationResult =
    writeIfMatching(pact, Success(results), config)
  
  def writeIfMatching(pact: Pact, tryResults: Try[PactSessionResults], config: PactConfig): VerificationResult = {
    for (results <- tryResults if results.allMatched) {
      PactGenerator.merge(pact).writeAllToFile(config)
    }
    VerificationResult(tryResults)
  }
  
  def runAndWritePact[T](pact: Pact, pactConfig: PactConfig = PactConfig(PactSpecVersion.V2))(userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val server = DefaultMockProvider.withDefaultConfig(pactConfig)
    new ConsumerPactRunner(server).runAndWritePact(pact, pactConfig)(userCode, userVerification)
  }
}

class ConsumerPactRunner(server: MockProvider) {
  import ConsumerPactRunner._
  
  def runAndWritePact[T](pact: Pact, config: PactConfig)(userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val tryResults = server.runAndClose(pact)(userCode)
    tryResults match {
      case Failure(e) =>
        if (server.session.remainingResults.allMatched) PactError(e)
        else PactMismatch(server.session.remainingResults, Some(e))
      case Success((codeResult, pactSessionResults)) => {
        userVerification(codeResult).fold(writeIfMatching(pact, pactSessionResults, config)) { error =>
          UserCodeFailed(error)
        }
      }
    }
  }
  
  def runAndWritePact(pact: Pact, userCode: Runnable): VerificationResult =
    runAndWritePact(pact, server.config.pactConfig)(userCode.run(), (u:Unit) => None)
  
}
