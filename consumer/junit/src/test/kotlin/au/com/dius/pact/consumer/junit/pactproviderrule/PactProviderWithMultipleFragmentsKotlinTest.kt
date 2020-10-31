package au.com.dius.pact.consumer.junit.pactproviderrule

import au.com.dius.pact.consumer.dsl.PactDslRequestWithoutPath
import au.com.dius.pact.consumer.dsl.PactDslResponse
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit.DefaultRequestValues
import au.com.dius.pact.consumer.junit.DefaultResponseValues
import au.com.dius.pact.consumer.junit.PactProviderRule
import au.com.dius.pact.consumer.junit.PactVerification
import au.com.dius.pact.consumer.junit.PactVerifications
import au.com.dius.pact.consumer.junit.exampleclients.ConsumerClient
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.support.unwrap
import org.apache.http.client.HttpResponseException
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PactProviderWithMultipleFragmentsKotlinTest {

  @Rule
  @JvmField
  var mockTestProvider = PactProviderRule("test_provider", this)

  @Rule
  @JvmField
  var mockTestProvider2 = PactProviderRule("test_provider2", this)

  @DefaultRequestValues
  fun defaultRequestValues(request: PactDslRequestWithoutPath) {
    request.headers(mapOf("testreqheader" to "testreqheadervalue"))
  }

  @DefaultResponseValues
  fun defaultResponseValues(response: PactDslResponse) {
    response.headers(mapOf("testresheader" to "testresheadervalue"))
  }

  @Pact(consumer = "test_consumer", provider = "test_provider")
  fun createFragment(builder: PactDslWithProvider): RequestResponsePact {
    return builder
      .given("good state")
      .uponReceiving("PactProviderTest test interaction")
        .path("/")
        .method("GET")
      .willRespondWith()
        .status(200)
        .body("{\"responsetest\": true, \"name\": \"harry\"}")
      .uponReceiving("PactProviderTest second test interaction")
        .method("OPTIONS")
        .path("/second")
        .body("")
      .willRespondWith()
        .status(200)
        .body("")
      .toPact().asRequestResponsePact().unwrap()
  }

  @Pact(consumer = "test_consumer", provider = "test_provider2")
  fun createFragment2(builder: PactDslWithProvider): RequestResponsePact {
    return builder
      .given("good state")
      .uponReceiving("PactProviderTest test interaction 2")
        .path("/")
        .method("GET")
      .willRespondWith()
        .status(200)
        .body("{\"responsetest\": true, \"name\": \"fred\"}")
      .toPact().asRequestResponsePact().unwrap()
  }

  @Pact(consumer = "test_consumer", provider = "test_provider2")
  fun createFragment3(builder: PactDslWithProvider): RequestResponsePact {
    return builder
      .given("bad state")
      .uponReceiving("PactProviderTest test interaction 3")
        .path("/path/2")
        .method("GET")
      .willRespondWith()
        .status(404)
        .body("{\"error\": \"ID 2 does not exist\"}")
      .toPact().asRequestResponsePact().unwrap()
  }

  @Test
  @PactVerification(value = ["test_provider2"], fragment = "createFragment2")
  @Throws(IOException::class)
  fun runTestWithFragment2() {
    val expectedResponse = mapOf("responsetest" to true, "name" to "fred")
    Assert.assertEquals(ConsumerClient(mockTestProvider2.url).getAsMap("/", ""), expectedResponse)
  }

  @Test
  @PactVerification(value = ["test_provider"], fragment = "createFragment")
  @Throws(IOException::class)
  fun runTestWithFragment1() {
    Assert.assertEquals(ConsumerClient(mockTestProvider.url).options("/second").toLong(), 200)
    val expectedResponse = mapOf("responsetest" to true, "name" to "harry")
    Assert.assertEquals(ConsumerClient(mockTestProvider.url).getAsMap("/", ""), expectedResponse)
  }

  @Test
  @PactVerifications(
    PactVerification(value = ["test_provider"], fragment = "createFragment"),
    PactVerification(value = ["test_provider2"], fragment = "createFragment2"))
  @Throws(IOException::class)
  fun runTestWithBothFragments() {
    Assert.assertEquals(ConsumerClient(mockTestProvider.url).options("/second").toLong(), 200)
    var expectedResponse = mapOf("responsetest" to true, "name" to "harry")
    Assert.assertEquals(ConsumerClient(mockTestProvider.url).getAsMap("/", ""), expectedResponse)
    expectedResponse = mapOf("responsetest" to true, "name" to "fred")
    Assert.assertEquals(ConsumerClient(mockTestProvider2.url).getAsMap("/", ""), expectedResponse)
  }

  @Test
  @PactVerifications(
    PactVerification(value = ["test_provider"], fragment = "createFragment"),
    PactVerification(value = ["test_provider2"], fragment = "createFragment2"),
    PactVerification(value = ["test_provider2"], fragment = "createFragment3"))
  @Throws(IOException::class)
  fun runTestWithAllFragments() {
    Assert.assertEquals(ConsumerClient(mockTestProvider.url).options("/second").toLong(), 200)
    var expectedResponse = mapOf("responsetest" to true, "name" to "harry")
    Assert.assertEquals(ConsumerClient(mockTestProvider.url).getAsMap("/", ""), expectedResponse)
    expectedResponse = mapOf("responsetest" to true, "name" to "fred")
    Assert.assertEquals(ConsumerClient(mockTestProvider2.url).getAsMap("/", ""), expectedResponse)
    try {
      ConsumerClient(mockTestProvider2.url).getAsMap("/path/2", "")
      Assert.fail()
    } catch (ex: HttpResponseException) {
      MatcherAssert.assertThat(ex.statusCode, Matchers.`is`(404))
    }
  }
}
