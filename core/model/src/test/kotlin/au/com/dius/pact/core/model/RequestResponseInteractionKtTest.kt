package au.com.dius.pact.core.model

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

open class RequestResponseInteractionKtTest {
  @Test
  fun pactSpecVersionAtLeast() {
    assertThat(null.atLeast(PactSpecVersion.V3), `is`(true))
    assertThat(PactSpecVersion.UNSPECIFIED.atLeast(PactSpecVersion.V3), `is`(true))
    assertThat(PactSpecVersion.V1.atLeast(PactSpecVersion.V3), `is`(false))
    assertThat(PactSpecVersion.V1_1.atLeast(PactSpecVersion.V3), `is`(false))
    assertThat(PactSpecVersion.V2.atLeast(PactSpecVersion.V3), `is`(false))
    assertThat(PactSpecVersion.V3.atLeast(PactSpecVersion.V3), `is`(true))
    assertThat(PactSpecVersion.V4.atLeast(PactSpecVersion.V3), `is`(true))
  }

  @Test
  fun pactSpecVersionLessThan() {
    assertThat(null.lessThan(PactSpecVersion.V3), `is`(false))
    assertThat(PactSpecVersion.UNSPECIFIED.lessThan(PactSpecVersion.V3), `is`(false))
    assertThat(PactSpecVersion.V1.lessThan(PactSpecVersion.V3), `is`(true))
    assertThat(PactSpecVersion.V1_1.lessThan(PactSpecVersion.V3), `is`(true))
    assertThat(PactSpecVersion.V2.lessThan(PactSpecVersion.V3), `is`(true))
    assertThat(PactSpecVersion.V3.lessThan(PactSpecVersion.V3), `is`(false))
    assertThat(PactSpecVersion.V4.lessThan(PactSpecVersion.V3), `is`(false))

    assertThat(null.lessThan(PactSpecVersion.V2), `is`(false))
    assertThat(PactSpecVersion.UNSPECIFIED.lessThan(PactSpecVersion.V2), `is`(false))
    assertThat(PactSpecVersion.V1.lessThan(PactSpecVersion.V2), `is`(true))
    assertThat(PactSpecVersion.V1_1.lessThan(PactSpecVersion.V2), `is`(true))
    assertThat(PactSpecVersion.V2.lessThan(PactSpecVersion.V2), `is`(false))
    assertThat(PactSpecVersion.V3.lessThan(PactSpecVersion.V3), `is`(false))
    assertThat(PactSpecVersion.V4.lessThan(PactSpecVersion.V3), `is`(false))

    assertThat(null.lessThan(PactSpecVersion.V4), `is`(true))
    assertThat(PactSpecVersion.UNSPECIFIED.lessThan(PactSpecVersion.V4), `is`(true))
    assertThat(PactSpecVersion.V1.lessThan(PactSpecVersion.V4), `is`(true))
    assertThat(PactSpecVersion.V1_1.lessThan(PactSpecVersion.V4), `is`(true))
    assertThat(PactSpecVersion.V2.lessThan(PactSpecVersion.V4), `is`(true))
    assertThat(PactSpecVersion.V3.lessThan(PactSpecVersion.V4), `is`(true))
    assertThat(PactSpecVersion.V4.lessThan(PactSpecVersion.V4), `is`(false))
  }
}
