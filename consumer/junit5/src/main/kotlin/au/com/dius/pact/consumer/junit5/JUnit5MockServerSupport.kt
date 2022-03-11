package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.AbstractBaseMockServer
import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.PactTestRun
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.PactSpecVersion
import org.junit.jupiter.api.extension.ExtensionContext

class JUnit5MockServerSupport(private val baseMockServer: BaseMockServer) : AbstractBaseMockServer(),
  ExtensionContext.Store.CloseableResource {
  override fun close() {
    baseMockServer.stop()
  }

  override fun start() = baseMockServer.start()
  override fun stop() = baseMockServer.stop()
  override fun waitForServer() = baseMockServer.waitForServer()
  override fun getUrl() = baseMockServer.getUrl()
  override fun getPort() = baseMockServer.getPort()
  override fun <R> runAndWritePact(pact: BasePact, pactVersion: PactSpecVersion, testFn: PactTestRun<R>) =
    baseMockServer.runAndWritePact(pact, pactVersion, testFn)
  override fun validateMockServerState(testResult: Any?) = baseMockServer.validateMockServerState(testResult)
}
