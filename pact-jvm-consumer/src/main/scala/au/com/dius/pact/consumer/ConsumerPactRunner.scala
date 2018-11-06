package au.com.dius.pact.consumer

import java.net.SocketException

import au.com.dius.pact.core.model.{Pact => PactModel, PactSpecVersion}
import au.com.dius.pact.core.model.RequestResponseInteraction

import scala.util.{Failure, Success, Try}

/**
  * @deprecated Moved to Kotlin implementation: use ConsumerPactRunnerKt.runConsumerTest
  */
@Deprecated
object ConsumerPactRunner {

  @Deprecated
  def writeIfMatching(pact: PactModel[RequestResponseInteraction], results: PactSessionResults, pactVersion: PactSpecVersion)
    : VerificationResult = writeIfMatching(pact, Success(results), pactVersion)

  @Deprecated
  def writeIfMatching(pact: PactModel[RequestResponseInteraction], tryResults: Try[PactSessionResults], pactVersion: PactSpecVersion): VerificationResult = {
    for (results <- tryResults if results.allMatched) {
      PactGenerator.merge(pact).writeAllToFile(pactVersion)
    }
    VerificationResult(tryResults)
  }

  @Deprecated
  def runAndWritePact[T](pact: PactModel[RequestResponseInteraction], pactVersion: PactSpecVersion = PactSpecVersion.V3)(userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val server = DefaultMockProvider.withDefaultConfig(pactVersion)
    new ConsumerPactRunner(server).runAndWritePact(pact, pactVersion)(userCode, userVerification)
  }
}

/**
  * @deprecated Moved to Kotlin implementation: use ConsumerPactRunnerKt.runConsumerTest
  */
@Deprecated
class ConsumerPactRunner(server: MockProvider[RequestResponseInteraction]) {
  import ConsumerPactRunner._

  @Deprecated
  def runAndWritePact[T](pact: PactModel[RequestResponseInteraction], pactVersion: PactSpecVersion)(userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val tryResults = server.runAndClose(pact)(userCode)
    tryResults match {
      case Failure(e) =>
        if (e.isInstanceOf[SocketException]) PactError(new MockServerException("Failed to start mock server: " + e.getMessage, e))
        else if (server.session.remainingResults.allMatched) PactError(e)
        else PactMismatch(server.session.remainingResults, Some(e))
      case Success((codeResult, pactSessionResults)) =>
        userVerification(codeResult).fold(writeIfMatching(pact, pactSessionResults, pactVersion)) { error =>
          UserCodeFailed(error)
        }
    }
  }

  @Deprecated
  def runAndWritePact(pact: PactModel[RequestResponseInteraction], userCode: Runnable): VerificationResult =
    runAndWritePact(pact, server.config.getPactVersion)(userCode.run(), (u:Unit) => None)

  @Deprecated
  def run[T](userCode: => T, userVerification: ConsumerTestVerification[T]): VerificationResult = {
    val tryResults = server.run(userCode)
    tryResults match {
      case Failure(e) =>
        PactError(e)
      case Success(codeResult) =>
        userVerification(codeResult).fold[VerificationResult](PactVerified)(UserCodeFailed(_))
    }
  }

  @Deprecated
  def writePact(pact: PactModel[RequestResponseInteraction], pactVersion: PactSpecVersion): VerificationResult =
    if (server.session.remainingResults.allMatched)
      writeIfMatching(pact, server.session.remainingResults, pactVersion)
    else
      PactMismatch(server.session.remainingResults, None)
}
