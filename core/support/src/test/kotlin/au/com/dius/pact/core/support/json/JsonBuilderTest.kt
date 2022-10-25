package au.com.dius.pact.core.support.json

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class JsonBuilderTest {
  @Test
  fun testEmptyBuilder() {
    val json = JsonBuilder.build {
      it
    }

    val expected = JsonValue.Object()

    assertThat(json, `is`(equalTo(expected)))
  }

  @Test
  fun testBasicJson() {
    val json = JsonBuilder.build {
      it["integer"] = 100
      it["decimal"] = 100.22
      it["string"] = "100.22"
      it["bool"] = true
      it["null"] = null
    }

    val expected = JsonValue.Object(mutableMapOf(
      "bool" to JsonValue.True,
      "decimal" to JsonValue.Decimal(100.22),
      "integer" to JsonValue.Integer(100),
      "null" to JsonValue.Null,
      "string" to JsonValue.StringValue("100.22")
    ))

    assertThat(json, `is`(equalTo(expected)))
  }

  @Test
  fun testChildObjectJson() {
    val json = JsonBuilder.build {
      it["child"] = it.`object` { child ->
        child["integer"] = 100
        child["decimal"] = 100.22
        child["string"] = "100.22"
        child["bool"] = true
        child["null"] = null
      }
    }

    val expected = JsonValue.Object(mutableMapOf("child" to JsonValue.Object(mutableMapOf(
      "bool" to JsonValue.True,
      "decimal" to JsonValue.Decimal(100.22),
      "integer" to JsonValue.Integer(100),
      "null" to JsonValue.Null,
      "string" to JsonValue.StringValue("100.22")
    ))))

    assertThat(json, `is`(equalTo(expected)))
  }

  @Test
  fun testChildArrayJson() {
    val json = JsonBuilder.build {
      it["child"] = it.array { child ->
        child.push(100.22)
        child += "100.22"
        child.push(null)
        child[2] = 100
      }
    }

    val expected = JsonValue.Object(mutableMapOf("child" to JsonValue.Array(
      mutableListOf(
        JsonValue.Decimal(100.22),
        JsonValue.StringValue("100.22"),
        JsonValue.Integer(100),
      )
    )))

    assertThat(json, `is`(equalTo(expected)))
  }

  @Test
  fun testChildMultiLevel() {
    val json = JsonBuilder.build {
      it["child"] = it.array { child ->
        child.push(child.`object` { child2 ->
          child2["term"] = true
        })
      }
    }

    val expected = JsonValue.Object(mutableMapOf("child" to JsonValue.Array(
      mutableListOf(JsonValue.Object("term" to JsonValue.True))
    )))

    assertThat(json, `is`(equalTo(expected)))
  }
}
