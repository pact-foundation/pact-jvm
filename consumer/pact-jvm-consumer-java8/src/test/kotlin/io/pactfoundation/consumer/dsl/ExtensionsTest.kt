package io.pactfoundation.consumer.dsl

import org.junit.jupiter.api.Test

class ExtensionsTest {
  @Test
  fun `can use LambdaDslJsonArray#newObject`() {
    newJsonArray { newObject { stringType("foo") } }
  }

  @Test
  fun `can use LambdaDslObject#newObject`() {
    newJsonObject {
      newObject("object") {
        stringType("field")
      }
    }
  }
}
