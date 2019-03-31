package io.pactfoundation.consumer.dsl

import org.junit.jupiter.api.Test

class ExtensionsTest {
  @Test
  fun `can use LambdaDslJsonArray#newObject`() {
    LambdaDsl.newJsonArray { array -> array.newObject { o -> o.stringType("foo") } }
  }
}
