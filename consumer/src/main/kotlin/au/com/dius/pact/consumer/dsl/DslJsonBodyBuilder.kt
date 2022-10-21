package au.com.dius.pact.consumer.dsl

import org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT
import java.time.ZonedDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure


class DslJsonBodyBuilder {
    companion object {
        private val ISO_PATTERN = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.pattern
    }

    /**
     * Build a {@link LambdaDslJsonBody} based on the Data Object required constructor fields
     */
    fun basedOnRequiredConstructorFields(kClass: KClass<*>): (LambdaDslJsonBody) -> Unit =
        { root: LambdaDslJsonBody ->
            root.run {
                val constructor = kClass.primaryConstructor
                fillBasedOnConstructorFields(constructor, root)
            }
        }

    private fun fillBasedOnConstructorFields(
        constructor: KFunction<Any>?,
        root: LambdaDslObject
    ) {
        constructor?.parameters?.filterNot { it.isOptional }?.forEach {
            when (val baseField = it.type.jvmErasure) {
                String::class -> root.stringType(it.name)
                Boolean::class -> root.booleanType(it.name)
                Byte::class,
                Short::class,
                Int::class,
                Long::class,
                Float::class,
                Number::class,
                Double::class ->
                    root.numberType(it.name)
                List::class -> root.array(it.name) {}
                ZonedDateTime::class -> root.datetime(it.name, ISO_PATTERN)
                else ->
                    root.`object`(it.name) { objDsl ->
                        objDsl.run {
                            fillBasedOnConstructorFields(
                                baseField.primaryConstructor,
                                objDsl
                            )
                        }
                    }
            }
        }
    }
}