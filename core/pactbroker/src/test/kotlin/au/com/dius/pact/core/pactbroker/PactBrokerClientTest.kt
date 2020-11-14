package au.com.dius.pact.core.pactbroker

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class PactBrokerClientTest {
  @Test
  fun retryWithWhenCountIsZeroRunsOnce() {
    var counter = 0
    val result = PactBrokerClient.retryWith("Test", 0, 0, { false }) {
      counter += 1
      counter
    }
    assertThat(result, equalTo(1))
  }

  @Test
  fun retryWithWhenCountIsOneRunsOnce() {
    var counter = 0
    val result = PactBrokerClient.retryWith("Test", 1, 0, { false }) {
      counter += 1
      counter
    }
    assertThat(result, equalTo(1))
  }

  @Test
  fun retryWithWhenCountIsGreaterThanOneButPredicateIsFalseRunsOnce() {
    var counter = 0
    val result = PactBrokerClient.retryWith("Test", 10, 0, { false }) {
      counter += 1
      counter
    }
    assertThat(result, equalTo(1))
  }

  @Test
  fun retryWithWhenCountIsGreaterThanOneAndPredicateIsTrueRunsTheNumberOfTimeByTheCount() {
    var counter = 0
    val result = PactBrokerClient.retryWith("Test", 10, 0, { true }) {
      counter += 1
      counter
    }
    assertThat(result, equalTo(10))
  }

  @Test
  fun retryWithWhenCountIsGreaterThanOneRunsUntilThePredicateIsFalse() {
    var counter = 0
    val result = PactBrokerClient.retryWith("Test", 10, 0, { v -> v < 5 }) {
      counter += 1
      counter
    }
    assertThat(result, equalTo(5))
  }
}
