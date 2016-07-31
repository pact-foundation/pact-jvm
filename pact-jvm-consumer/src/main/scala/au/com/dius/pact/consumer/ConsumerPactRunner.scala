package au.com.dius.pact.consumer

import au.com.dius.pact.model.{Pact, PactSpecVersion}

import scala.util.{Failure, Success, Try}

object ConsumerPactRunner {
  
  def writeIfMatching(pact: Pact, results: PactSessionResults, pactVersion: PactSpecVersion): VerificationResult =
    writeIfMatching(pact, Success(results), pactVersion)
  
  def writeIfMatching(pact: Pact, tryResults: Try[PactSessionResults], pactVersion: PactSpecVersion): VerificationResult = {
    for (results <- tryResults if results.allMatched) {
      PactGenerator.merge(pact).writeAllToFile(pactVersion)
    }
    VerificationResult(tryResults)
  }
  
  def runAndWritePact[T](pact: Pact, pactVersion: PactSpecVersion = PactSpecVersion.V2)(userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val server = DefaultMockProvider.withDefaultConfig(pactVersion)
    new ConsumerPactRunner(server).runAndWritePact(pact, pactVersion)(userCode, userVerification)
  }
}

class ConsumerPactRunner(server: MockProvider) {
  import ConsumerPactRunner._
  
  def runAndWritePact[T](pact: Pact, pactVersion: PactSpecVersion)(userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val tryResults = server.runAndClose(pact)(userCode)
    tryResults match {
      case Failure(e) =>
        if (server.session.remainingResults.allMatched) PactError(e)
        else PactMismatch(server.session.remainingResults, Some(e))
      case Success((codeResult, pactSessionResults)) => {
        userVerification(codeResult).fold(writeIfMatching(pact, pactSessionResults, pactVersion)) { error =>
          UserCodeFailed(error)
        }
      }
    }
  }
  
  def runAndWritePact(pact: Pact, userCode: Runnable): VerificationResult =
    runAndWritePact(pact, server.config.pactVersion)(userCode.run(), (u:Unit) => None)
  
}
