package io.pactfoundation.consumer.dsl

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.stream.Stream

internal class DslJsonBodyBuilderTest {
    private fun basedOnConstructor(classTest: KClass<*>) =
        DslJsonBodyBuilder().basedOnRequiredConstructorFields(classTest)

    @ParameterizedTest
    @MethodSource(value = ["stringPropertyOptionalProperties"])
    internal fun `should not map string property optional with default constructor`(
        classTest: KClass<*>
    ) {
        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(classTest))

        val expectedBody =
            LambdaDsl.newJsonBody { }

        assertThat(actualJsonBody.pactDslObject.toString())
            .isEqualTo(expectedBody.pactDslObject.toString())
    }

    @ParameterizedTest
    @MethodSource(value = ["stringPropertyNonOptionalProperties"])
    internal fun `should map string property non-optional`(classTest: KClass<*>) {
        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(classTest))

        val expectedBody =
            LambdaDsl.newJsonBody { it.stringType("property") }

        assertThat(actualJsonBody.pactDslObject.toString())
            .isEqualTo(expectedBody.pactDslObject.toString())
    }

    @Test
    internal fun `should map string property non-optional with var`() {
        data class StringObjectRequiredProperty(var property: String)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(StringObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.stringType("property") }

        assertThat(actualJsonBody.pactDslObject.toString())
            .isEqualTo(expectedBody.pactDslObject.toString())
    }

    @Test
    internal fun `should map boolean property non-optional`() {
        data class BooleanObjectRequiredProperty(val property: Boolean)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(BooleanObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.booleanType("property") }

        assertThat(actualJsonBody.pactDslObject.toString())
            .isEqualTo(expectedBody.pactDslObject.toString())
    }

    @ParameterizedTest
    @MethodSource(value = ["numberPropertyNonOptional"])
    internal fun `should map simple number property non-optional`(classTest: KClass<*>) {
        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(classTest))

        val expectedBody =
            LambdaDsl.newJsonBody { it.numberType("property") }

        assertThat(actualJsonBody.pactDslObject.toString())
            .isEqualTo(expectedBody.pactDslObject.toString())
    }

    @Test
    internal fun `should map zoned date time field for iso 8601`() {
        data class DatetimeRequiredProperty(val property: ZonedDateTime)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(DatetimeRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.datetime("property", "yyyy-MM-dd'T'HH:mm:ssZZ") }

        assertThat(actualJsonBody.pactDslObject.toString())
            .isEqualTo(expectedBody.pactDslObject.toString())
    }

    @Test
    internal fun `should map array field`() {
        data class ListObjectRequiredProperty(val property: List<String>)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(ListObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.array("property") {} }

        assertThat(actualJsonBody.pactDslObject.toString())
            .isEqualTo(expectedBody.pactDslObject.toString())
    }

    @Test
    internal fun `should map inner object`() {
        data class InnerObjectRequiredProperty(val property: String)
        data class ObjectRequiredProperty(val inner: InnerObjectRequiredProperty)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(ObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { root ->
                root.`object`("inner") { it.stringType("property") }
            }

        assertThat(actualJsonBody.pactDslObject.toString())
            .isEqualTo(expectedBody.pactDslObject.toString())
    }

    companion object {
        @JvmStatic
        private fun numberPropertyNonOptional(): Stream<KClass<*>> {
            data class ByteObjectNonRequiredProperty(val property: Byte)
            data class ShortObjectNonRequiredProperty(val property: Short)
            data class IntObjectNonRequiredProperty(val property: Int)
            data class LongObjectNonRequiredProperty(val property: Long)
            data class FloatObjectNonRequiredProperty(val property: Float)
            data class DoubleObjectNonRequiredProperty(val property: Double)
            data class NumberObjectNonRequiredProperty(val property: Number)

            return Stream.of(
                ByteObjectNonRequiredProperty::class,
                ShortObjectNonRequiredProperty::class,
                IntObjectNonRequiredProperty::class,
                LongObjectNonRequiredProperty::class,
                FloatObjectNonRequiredProperty::class,
                DoubleObjectNonRequiredProperty::class,
                NumberObjectNonRequiredProperty::class
            )
        }

        @JvmStatic
        private fun stringPropertyOptionalProperties(): Stream<KClass<*>> {
            data class StringObjectNonRequiredPropertyImmutable(val property: String = "")
            data class StringObjectNonRequiredPropertyMutable(var property: String = "")

            return Stream.of(
                StringObjectNonRequiredPropertyImmutable::class,
                StringObjectNonRequiredPropertyMutable::class
            )
        }

        @JvmStatic
        private fun stringPropertyNonOptionalProperties(): Stream<KClass<*>> {
            data class StringObjectRequiredPropertyImmutable(val property: String)
            data class StringObjectRequiredPropertyMutable(var property: String)

            return Stream.of(
                StringObjectRequiredPropertyImmutable::class,
                StringObjectRequiredPropertyMutable::class
            )
        }
    }
}
