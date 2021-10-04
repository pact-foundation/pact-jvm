package au.com.dius.pact.provider.junit

import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junit.target.MessageTarget
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junitsupport.target.TestTarget
import au.com.dius.pact.provider.junitsupport.{Provider, State}
import org.junit.runner.RunWith

@RunWith(classOf[PactRunner])
@Provider("AmqpProvider")
@PactFolder("src/test/resources/amqp_pacts")
class ScalaJunitTest {
  @TestTarget final val target = new MessageTarget

  @State(Array("SomeProviderState")) def someProviderState(): Unit = {
  }

  @PactVerifyProvider("a test message") def verifyMessageForOrder = "{\"testParam1\": \"value1\",\"testParam2\": \"value2\"}"
}
