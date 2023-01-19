package au.com.dius.pact.consumer.dsl

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass
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

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @ParameterizedTest
    @MethodSource(value = ["stringPropertyNonOptionalProperties"])
    internal fun `should map string property non-optional`(classTest: KClass<*>) {
        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(classTest))

        val expectedBody =
            LambdaDsl.newJsonBody { it.stringType("property") }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @Test
    internal fun `should map string property non-optional with var`() {
        data class StringObjectRequiredProperty(var property: String)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(StringObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.stringType("property") }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @Test
    internal fun `should map boolean property non-optional`() {
        data class BooleanObjectRequiredProperty(val property: Boolean)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(BooleanObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.booleanType("property") }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @ParameterizedTest
    @MethodSource(value = ["numberPropertyNonOptional"])
    internal fun `should map simple number property non-optional`(classTest: KClass<*>) {
        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(classTest))

        val expectedBody =
            LambdaDsl.newJsonBody { it.numberType("property") }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @Test
    internal fun `should map zoned date time field for iso 8601`() {
        data class DatetimeRequiredProperty(val property: ZonedDateTime)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(DatetimeRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.datetime("property", "yyyy-MM-dd'T'HH:mm:ssZZ") }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @Test
    internal fun `should map array field with non empty set for string type`() {
        data class ListObjectRequiredProperty(val properties: List<String>)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(ListObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.array("properties") { arr ->
                arr.stringType("String")
            } }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @Test
    internal fun `should map array field with non empty set for boolean type`() {
        data class ListObjectRequiredProperty(val properties: List<Boolean>)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(ListObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.array("properties") { arr ->
                arr.booleanType(true)
            } }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @Test
    internal fun `should map array field with non empty set for datetime type`() {
        data class ListObjectRequiredProperty(val properties: List<ZonedDateTime>)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(ListObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.array("properties") { arr ->
                arr.datetimeExpression("now", "yyyy-MM-dd'T'HH:mm:ssZZ")
            } }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @ParameterizedTest
    @MethodSource(value = ["listWithNumberProperties"])
    internal fun `should map array field with non empty set for number type`(classTest: KClass<*>) {
        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(classTest))

        val expectedBody =
            LambdaDsl.newJsonBody { it.array("properties") { arr ->
                arr.numberType(1)
            } }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @Test
    internal fun `should map array field with list of object`() {
        data class InnerObject(val property: String)
        data class ListObjectRequiredProperty(val properties: List<InnerObject>)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(ListObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.array("properties") { arr ->
                arr.`object` { obj ->
                    obj.stringType("property")
                }
            } }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @Test
    internal fun `should map array field with inner object without optional property`() {
        data class InnerObject(val property: String = "")
        data class ListObjectRequiredProperty(val properties: List<InnerObject>)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(ListObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { it.array("properties") { arr ->
                arr.`object` { }
            } }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    data class SecondListProperty(val third: List<FirstListProperty>, val property: String)
    data class FirstListProperty(val second: List<SecondListProperty>)
    @Test
    internal fun `should map array inner object with loop reference keeping other fields`() {
        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(FirstListProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { root ->
                root.array("second") {
                    it.`object` { sec ->
                        sec.array("third") { loop -> loop.`object`{ } }
                        sec.stringType("property")
                    }
                }
            }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
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

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @Test
    internal fun `should map inner object multiple occurrences`() {
        data class InnerObjectRequiredProperty(val property: String)
        data class ObjectRequiredProperty(val inner: InnerObjectRequiredProperty,
                                          val second: InnerObjectRequiredProperty)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(ObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { root ->
                root.`object`("inner") {
                    it.stringType("property")
                }
                root.`object`("second") {
                    it.stringType("property")
                }
            }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    data class InnerObjectRequiredProperty(val property: ObjectRequiredProperty)
    data class ObjectRequiredProperty(val inner: InnerObjectRequiredProperty)
    @Test
    internal fun `should map inner object with loop reference for the first level`() {
        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(ObjectRequiredProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { root ->
                root.`object`("inner") { it.`object`("property") { } }
            }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    data class ThirdProperty(val dependOnFirst: FirstProperty, val property: String)
    data class SecondProperty(val third: ThirdProperty)
    data class FirstProperty(val second: SecondProperty)
    @Test
    internal fun `should map inner object with loop reference keeping other fields`() {
        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(FirstProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { root ->
                root.`object`("second") {
                    it.`object`("third") { loop ->
                        loop.`object`("dependOnFirst") { }
                        loop.stringType("property")
                    }
                }
            }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    @Test
    internal fun `should map inner object reusing the same class internally`() {
        data class CommonClass(val name: String)
        data class ThirdToUseCommonProperty(val common: CommonClass)
        data class SecondToUseCommonProperty(val third: ThirdToUseCommonProperty, val common: CommonClass)
        data class FirstToUseCommonProperty(val second: SecondToUseCommonProperty)

        val actualJsonBody = LambdaDsl.newJsonBody(basedOnConstructor(FirstToUseCommonProperty::class))

        val expectedBody =
            LambdaDsl.newJsonBody { root ->
                root.`object`("second") {
                    it.`object`("third") { loop ->
                        loop.`object`("common") { third -> third.stringType("name") }
                    }
                    it.`object`("common") { loop ->
                        loop.stringType("name")
                    }
                }
            }

        assertThat(actualJsonBody.pactDslObject.toString(), `is`(equalTo(expectedBody.pactDslObject.toString())))
    }

    companion object {
        @JvmStatic
        @Suppress("UnusedPrivateMember")
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
                NumberObjectNonRequiredProperty::class,
            )
        }

        @JvmStatic
        @Suppress("UnusedPrivateMember")
        private fun stringPropertyOptionalProperties(): Stream<KClass<*>> {
            data class StringObjectNonRequiredPropertyImmutable(val property: String = "")
            data class StringObjectNonRequiredPropertyMutable(var property: String = "")

            return Stream.of(
                StringObjectNonRequiredPropertyImmutable::class,
                StringObjectNonRequiredPropertyMutable::class,
            )
        }

        @JvmStatic
        @Suppress("UnusedPrivateMember")
        private fun stringPropertyNonOptionalProperties(): Stream<KClass<*>> {
            data class StringObjectRequiredPropertyImmutable(val property: String)
            data class StringObjectRequiredPropertyMutable(var property: String)

            return Stream.of(
                StringObjectRequiredPropertyImmutable::class,
                StringObjectRequiredPropertyMutable::class,
            )
        }

        @JvmStatic
        @Suppress("UnusedPrivateMember")
        private fun listWithNumberProperties(): Stream<KClass<*>> {
            data class ByteObjectNonRequiredProperty(val properties: List<Byte>)
            data class ShortObjectNonRequiredProperty(val properties: List<Short>)
            data class IntObjectNonRequiredProperty(val properties: List<Int>)
            data class LongObjectNonRequiredProperty(val properties: List<Long>)
            data class FloatObjectNonRequiredProperty(val properties: List<Float>)
            data class DoubleObjectNonRequiredProperty(val properties: List<Double>)
            data class NumberObjectNonRequiredProperty(val properties: List<Number>)

            return Stream.of(
                ByteObjectNonRequiredProperty::class,
                ShortObjectNonRequiredProperty::class,
                IntObjectNonRequiredProperty::class,
                LongObjectNonRequiredProperty::class,
                FloatObjectNonRequiredProperty::class,
                DoubleObjectNonRequiredProperty::class,
                NumberObjectNonRequiredProperty::class,
            )
        }
    }
}
