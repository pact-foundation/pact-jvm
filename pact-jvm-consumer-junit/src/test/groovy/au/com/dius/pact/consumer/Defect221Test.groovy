package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.model.RequestResponsePact
import groovy.json.JsonSlurper
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.junit.Rule
import org.junit.Test

class Defect221Test {

    private static final String APPLICATION_JSON = 'application/json'

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final PactProviderRuleMk2 provider = new PactProviderRuleMk2('221_provider', 'localhost', 8081, this)

    @Pact(provider= '221_provider', consumer= 'test_consumer')
    @SuppressWarnings('JUnitPublicNonTestMethod')
    RequestResponsePact createFragment(PactDslWithProvider builder) {
        builder
            .given('test state')
            .uponReceiving('A request with double precision number')
                .path('/numbertest')
                .method('PUT')
                .body('{"name": "harry","data": 1234.0 }', APPLICATION_JSON)
            .willRespondWith()
                .status(200)
                .body('{"responsetest": true, "name": "harry","data": 1234.0 }', APPLICATION_JSON)
            .toPact()
    }

    @Test
    @PactVerification('221_provider')
    void runTest() {
        assert [responsetest: true, name: 'harry', data: 1234.0] ==
          new JsonSlurper().parseText(Request.Put('http://localhost:8081/numbertest')
            .addHeader('Accept', APPLICATION_JSON)
            .bodyString('{"name": "harry","data": 1234.0 }', ContentType.APPLICATION_JSON)
            .execute().returnContent().asString())
    }
}
