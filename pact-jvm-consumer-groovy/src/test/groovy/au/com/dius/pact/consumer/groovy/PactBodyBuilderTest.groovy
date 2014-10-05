package au.com.dius.pact.consumer.groovy

import org.junit.Test

class PactBodyBuilderTest {
    @Test
    void dsl() {
        def service = new PactBuilder()
        service {
            serviceConsumer "Consumer"
            hasPactWith "Provider"
        }
        service {
            uponReceiving('a request')
            withAttributes(method: 'get', path: '/')
            withBody {
              name(~/\w+/, 'harry')
              surname regexp(~/\w+/, 'larry')
              position regexp(~/staff|contractor/)
              happy(true)

              hexCode hexValue
              hexCode2 hexValue('01234AB')
              id identifier
              id2 identifier('1234567890')
              localAddress ipAddress
              localAddress2 ipAddress('192.169.0.2')
              age 100
              age2 numeric
              timestamp timestamp

              values [1, 2, 3, numeric]

              role {
                name 'admin'
                id guid
              }
            }
            willRespondWith(
                status: 200,
                headers: ['Content-Type': 'text/html']
            )
            withBody {
              name(~/\w+/, 'harry')
            }
        }
        service.buildInteractions()
        assert service.interactions.size() == 1
        assert service.interactions[0].request.matchers.get() == [:]
        assert service.interactions[0].request.body.get() == ""
        assert service.interactions[0].response.matchers.get() == [:]
        assert service.interactions[0].response.body.get() == ""
    }
}
