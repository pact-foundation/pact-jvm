package au.com.dius.pact.model

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class OptionalBodyTest : StringSpec() {

  val nullBodyVar: OptionalBody? = null
  val missingBody = OptionalBody.missing()
  val nullBody = OptionalBody.nullBody()
  val emptyBody = OptionalBody.empty()
  val presentBody = OptionalBody.body("present")

  init {

    "a null body variable is missing" {
      nullBodyVar.isMissing() shouldBe true
    }

    "a missing body is missing" {
      missingBody.isMissing() shouldBe true
    }

    "a body that contains a null is not missing" {
      nullBody.isMissing() shouldBe false
    }

    "an empty body is not missing" {
      emptyBody.isMissing() shouldBe false
    }

    "a present body is not missing" {
      presentBody.isMissing() shouldBe false
    }

    "a null body variable is null" {
      nullBodyVar.isNull() shouldBe true
    }

    "a missing body is not null" {
      missingBody.isNull() shouldBe false
    }

    "a body that contains a null is null" {
      nullBody.isNull() shouldBe true
    }

    "an empty body is not null" {
      emptyBody.isNull() shouldBe false
    }

    "a present body is not null" {
      presentBody.isNull() shouldBe false
    }

    "a null body variable is not empty" {
      nullBodyVar.isEmpty() shouldBe false
    }

    "a missing body is not empty" {
      missingBody.isEmpty() shouldBe false
    }

    "a body that contains a null is not empty" {
      nullBody.isEmpty() shouldBe false
    }

    "an empty body is empty" {
      emptyBody.isEmpty() shouldBe true
    }

    "a present body is not empty" {
      presentBody.isEmpty() shouldBe false
    }

    "a null body variable is not present" {
      nullBodyVar.isPresent() shouldBe false
    }

    "a missing body is not present" {
      missingBody.isPresent() shouldBe false
    }

    "a body that contains a null is not present" {
      nullBody.isPresent() shouldBe false
    }

    "an empty body is not present" {
      emptyBody.isPresent() shouldBe false
    }

    "a present body is present" {
      presentBody.isPresent() shouldBe true
    }

    "a null body or else returns the else" {
      nullBodyVar.orElse("else") shouldBe "else"
    }

    "a missing body or else returns the else" {
      missingBody.orElse("else") shouldBe "else"
    }

    "a body that contains a null or else returns the else" {
      nullBody.orElse("else") shouldBe "else"
    }

    "an empty body or else returns empty" {
      emptyBody.orElse("else") shouldBe ""
    }

    "a present body or else returns the body" {
      presentBody.orElse("else") shouldBe "present"
    }
  }
}
