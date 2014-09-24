package au.com.dius.pact.consumer.groovy
import org.junit.Test
import scala.None$

class PactBuilderTest {
    @Test
    def void "should not define providerState when no given()"() {
        def alice_service = new PactBuilder()
        alice_service {
            service_consumer "Consumer"
            has_pact_with "Alice Service"
            port 1234
        }
        alice_service {
            upon_receiving('a retrieve Mallory request')
            with(method: 'get', path: '/mallory')
            will_respond_with(
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
