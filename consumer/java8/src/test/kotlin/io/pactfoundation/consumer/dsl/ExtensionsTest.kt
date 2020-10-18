package io.pactfoundation.consumer.dsl

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

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
}
