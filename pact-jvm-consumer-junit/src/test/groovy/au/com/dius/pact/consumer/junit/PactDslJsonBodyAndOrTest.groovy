package au.com.dius.pact.consumer.junit

import au.com.dius.pact.consumer.ConsumerPactTestMk2
import au.com.dius.pact.consumer.MatcherTestUtils
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.PM
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.exampleclients.ConsumerClient
import au.com.dius.pact.model.RequestResponsePact

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasKey
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue

class PactDslJsonBodyAndOrTest extends ConsumerPactTestMk2 {

    @Override
    protected RequestResponsePact createPact(PactDslWithProvider builder) {
        DslPart body = new PactDslJsonBody()
          .numberValue('valueA', 100)
          .and('valueB', 'AB', PM.includesStr('A'), PM.includesStr('B'))
          .or('valueC', null, PM.date(), PM.nullValue())
        RequestResponsePact pact = builder
          .uponReceiving('java test interaction with ands and ors')
            .path('/')
            .method('GET')
          .willRespondWith()
            .status(200)
            .body(body)
          .toPact()

        MatcherTestUtils.assertResponseMatcherKeysEqualTo(pact, 'body',
          '$.valueB',
          '$.valueC')

        pact
    }

    @Override
    protected String providerName() {
        'test_provider'
    }

    @Override
    protected String consumerName() {
        'test_consumer'
    }

    @Override
    protected void runTest(MockServer mockServer) {
        Map response = new ConsumerClient(mockServer.url).getAsMap('/', '')
        assertThat(response, hasKey('valueA'))
        assertThat(response, hasKey('valueB'))
        assertThat(response, hasKey('valueC'))
        assertThat(response.get('valueC'), is(nullValue()))
    }
}
