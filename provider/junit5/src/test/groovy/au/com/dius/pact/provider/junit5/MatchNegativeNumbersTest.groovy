package au.com.dius.pact.provider.junit5

import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith

@Provider('connector')
@PactFolder('src/test/resources/amqp_pacts')
@Slf4j
class MatchNegativeNumbersTest {

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider)
  void testTemplate(PactVerificationContext context) {
    context.verifyInteraction()
  }

  @BeforeEach
  void before(PactVerificationContext context) {
    context.setTarget(new MessageTestTarget())
  }

  @PactVerifyProvider('a dispute lost event')
  String disputeLostEvent() {
    JsonOutput.toJson([
      event_type: 'DISPUTE_LOST',
      service_id: 'service-id',
      resource_type: 'dispute',
      event_details: [
        gateway_account_id: 'a-gateway-account-id',
        net_amount: -8000,
        amount: 6500,
        fee: 1500
      ],
      live: true,
      timestamp: '2022-01-19T07:59:20.000000Z',
      resource_external_id: 'payment-external-id',
      parent_resource_external_id: 'external-id'
    ])
  }
}
