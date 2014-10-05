package au.com.dius.pact.consumer.groovy
import org.junit.Test
import scala.None$

class PactBuilderTest {
    @Test
    void "should not define providerState when no given()"() {
        def alice_service = new PactBuilder()
        alice_service {
            serviceConsumer "Consumer"
            hasPactWith "Alice Service"
            port 1234
        }
        alice_service {
            uponReceiving('a retrieve Mallory request')
            withAttributes(method: 'get', path: '/mallory')
            willRespondWith(
                    status: 200,
                    headers: ['Content-Type': 'text/html'],
                    body: '"That is some good Mallory."'
            )
        }
        alice_service.buildInteractions()
        assert alice_service.interactions.size() == 1
        assert alice_service.interactions[0].providerState == None$.empty()
    }
}
