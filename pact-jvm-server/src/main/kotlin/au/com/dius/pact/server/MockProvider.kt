package au.com.dius.pact.server

import au.com.dius.pact.consumer.model.MockHttpsKeystoreProviderConfig
import au.com.dius.pact.consumer.model.MockHttpsProviderConfig
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.support.handleWith
import au.com.dius.pact.core.support.Result
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

interface MockProvider {
  val config: MockProviderConfig
  val session: PactSession
  fun start(pact: Pact)
  fun <T> run(code: () -> T): Result<T, Exception>
  fun <T> runAndClose(pact: Pact, code: () -> T): Result<Pair<T, PactSessionResults>, Exception>
  fun stop()
}

object DefaultMockProvider {

  fun withDefaultConfig(pactVersion: PactSpecVersion = PactSpecVersion.V3) =
    apply(MockProviderConfig.createDefault(pactVersion))

  // Constructor providing a default implementation of StatefulMockProvider.
  // Users should not explicitly be forced to choose a variety.
  fun apply(config: MockProviderConfig): StatefulMockProvider =
    when (config) {
      is MockHttpsProviderConfig -> KTorHttpsKeystoreMockProvider(config)
//      is MockHttpsProviderConfig -> UnfilteredHttpsMockProvider(config)
      else -> KTorMockProvider(config)
    }
}

abstract class StatefulMockProvider: MockProvider {
  private var sessionVar = PactSession.empty
  private var pactVar: Pact? = null

  private fun waitForRequestsToFinish() = Thread.sleep(100)

  override val session: PactSession
    get() = sessionVar
  val pact: Pact?
    get() = pactVar

  abstract fun start()

  @Synchronized
  override fun start(pact: Pact) {
    pactVar = pact
    sessionVar = PactSession.forPact(pact)
    start()
  }

  override fun <T> run(code: () -> T): Result<T, Exception> {
    return handleWith {
      val codeResult = code()
      waitForRequestsToFinish()
      codeResult
    }
  }

  override fun <T> runAndClose(pact: Pact, code: () -> T): Result<Pair<T, PactSessionResults>, Exception> {
    return handleWith {
      try {
        start(pact)
        val codeResult = code()
        waitForRequestsToFinish()
        (codeResult to session.remainingResults())
      } finally {
        stop()
      }
    }
  }

  @Synchronized
  fun handleRequest(req: Request): IResponse {
    logger.debug { "Received request: $req" }
    val (response, newSession) = session.receiveRequest(req)
    logger.debug { "Generating response: $response" }
    sessionVar = newSession
    return response
  }
}
