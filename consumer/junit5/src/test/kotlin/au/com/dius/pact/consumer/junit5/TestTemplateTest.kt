package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody
import au.com.dius.pact.consumer.dsl.PactBuilder
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(value = [PactConsumerTestExt::class])
@PactTestFor(providerName = "checkout-service", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
class TestTemplateTest {
  companion object {
    private val reservationBody = newJsonBody { o ->
      o.stringType("purchaseId", "111")
      o.stringType("name", "PURCHASE_STARTED")
      o.eachLike("products", 1) { items ->
        items.stringType("productID", "1")
        items.stringType("productType", "FLIGHT")
        items.stringType("availabilityId", "28e80c5987c6a242516ccdc004235b5e")
      }
    }.build()

    private val cancelationBody = newJsonBody { o ->
      o.stringType("purchaseId", "111")
      o.stringType("reason", "user canceled")
    }.build()

    @JvmStatic
    @Pact(consumer = "reservation-service", provider = "checkout-service")
    fun pactForReservationBooking(builder: PactBuilder): V4Pact {
      return builder
              .usingLegacyMessageDsl()
              .hasPactWith("checkout-service")
              .expectsToReceive("a purchase started message to book a reservation")
              .withContent(reservationBody)
              .toPact()
    }

    @JvmStatic
    @Pact(consumer = "reservation-service", provider = "checkout-service")
    fun pactForCancellationBooking(builder: PactBuilder): V4Pact {
      return builder
              .usingLegacyMessageDsl()
              .hasPactWith("checkout-service")
              .expectsToReceive("a cancellation message to cancel a reservation")
              .withContent(cancelationBody)
              .toPact()
    }
  }

  @TestTemplate
  fun testPactForReservationBooking(message: V4Interaction.AsynchronousMessage) {
    assertThat(message, `is`(notNullValue()))
  }
}
