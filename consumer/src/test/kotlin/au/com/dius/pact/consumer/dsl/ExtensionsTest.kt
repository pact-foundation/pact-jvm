package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.util.List
import java.util.Map

class ExtensionsTest {
    @Test
    fun `can use Kotlin DSL to create a Json Array`() {
        val expectedJson = """[
        |{"key":"value"},
        |{"key_1":"value_1"}
        |]""".trimMargin().replace("\n", "")

        val actualJson = newJsonArray {
          newObject { stringValue("key", "value") }
          newObject { stringValue("key_1", "value_1") }
        }.body.toString()

        assertThat(actualJson, equalTo(expectedJson))
    }

    @Test
    fun `can use Kotlin DSL to create a Json`() {
        val expectedJson = """{
        |"array":[{"key":"value"}],
        |"object":{"property":"value"}
        |}""".trimMargin().replace("\n", "")

        val actualJson = newJsonObject {
          newArray("array") {
            newObject { stringValue("key", "value") }
          }
          newObject("object") {
            stringValue("property", "value")
          }
        }.body.toString()

        assertThat(actualJson, equalTo(expectedJson))
    }

    @Test
    fun `can use Kotlin DSL to create a Json body based on required constructor args`() {
        data class DataClassObject(val string: String, val number: Number, val optional: String? = null)

        val expectedJson = """
            |{"number":100,"string":"string"}
            |""".trimMargin().replace("\n", "")

        val actualJson = newJsonObject(DataClassObject::class).body.toString()

        assertThat(actualJson, equalTo(expectedJson))
    }

  // Issue #1796
  @Test
  fun `allow dsl to be extended from a common base`() {
    val x = newJsonObject {
      stringType("a", "foo")
      id("b", 0L)
      integerType("c", 0)
      booleanType("d", false)
    }
    val y = newJsonObject(x) {
      stringType("e", "bar")
    }
    val z = newJsonObject(x) {
      nullValue("e")
    }

    val expectedY = "{\"a\":\"foo\",\"b\":0,\"c\":0,\"d\":false,\"e\":\"bar\"}"
    val yRules = Map.of(
      "$.a", MatchingRuleGroup(List.of(TypeMatcher)),
      "$.b", MatchingRuleGroup(List.of(TypeMatcher)),
      "$.c", MatchingRuleGroup(List.of(NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))),
      "$.d", MatchingRuleGroup(List.of(TypeMatcher)),
      "$.e", MatchingRuleGroup(List.of(TypeMatcher))
    )
    val expectedYMatchers = MatchingRuleCategory("body", yRules)
    val expectedZ = "{\"a\":\"foo\",\"b\":0,\"c\":0,\"d\":false,\"e\":null}"
    val zRules = Map.of(
      "$.a", MatchingRuleGroup(List.of(TypeMatcher)),
      "$.b", MatchingRuleGroup(List.of(TypeMatcher)),
      "$.c", MatchingRuleGroup(List.of(NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))),
      "$.d", MatchingRuleGroup(List.of(TypeMatcher))
    )
    val expectedZMatchers = MatchingRuleCategory("body", zRules)

    assertThat(y.body.toString(), CoreMatchers.`is`(expectedY))
    assertThat(y.matchers, CoreMatchers.`is`(expectedYMatchers))
    assertThat(z.body.toString(), CoreMatchers.`is`(expectedZ))
    assertThat(z.matchers, CoreMatchers.`is`(expectedZMatchers))
  }
}
