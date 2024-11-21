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
        private const val NUMBER_EXAMPLE = 1
        private const val DATETIME_EXPRESSION_EXAMPLE = "now"
        private const val BOOLEAN_EXAMPLE = true
    }

    /**
     * Build a {@link LambdaDslJsonBody} based on the required constructor fields
     */
    fun basedOnRequiredConstructorFields(kClass: KClass<*>): (LambdaDslJsonBody) -> Unit =
        { root: LambdaDslJsonBody ->
            root.run {
                val constructor = kClass.primaryConstructor
                fillBasedOnConstructorFields(constructor, root, setOf(kClass))
            }
        }

    private fun fillBasedOnConstructorFields(
        constructor: KFunction<Any>?,
        root: LambdaDslObject,
        alreadyProcessedObject: Set<KClass<*>> = setOf()
    ) {
        constructor?.parameters?.filterNot { it.isOptional }?.forEach {
            when (val baseField = it.type.jvmErasure) {
                Byte::class,
                Short::class,
                Int::class,
                Long::class,
                Float::class,
                Number::class,
                Double::class -> root.numberType(it.name)
                String::class -> root.stringType(it.name)
                Boolean::class -> root.booleanType(it.name)
                ZonedDateTime::class -> root.datetime(it.name, ISO_PATTERN)
                List::class -> root.array(it.name) { arr ->
                    arr.run {
                        fillBasedOnConstructorFields(
                            it.type.arguments.first().type?.jvmErasure,
                            arr,
                            alreadyProcessedObject + baseField
                        )
                    }
                }
                else ->
                    root.`object`(it.name) { objDsl ->
                        objDsl.run {
                            if (!alreadyProcessedObject.contains(baseField)){
                                fillBasedOnConstructorFields(
                                    baseField.primaryConstructor,
                                    objDsl,
                                    alreadyProcessedObject + baseField
                                )
                            }
                        }
                    }
            }
        }
    }

    private fun fillBasedOnConstructorFields(
        listTypeCLass:  KClass<*>?,
        rootArray: LambdaDslJsonArray,
        alreadyProcessedObject: Set<KClass<*>> = setOf()
    ) {
        when(listTypeCLass) {
            Byte::class,
            Short::class,
            Int::class,
            Long::class,
            Float::class,
            Number::class,
            Double::class -> rootArray.numberType(NUMBER_EXAMPLE)
            String::class -> rootArray.stringType(listTypeCLass.simpleName)
            Boolean::class -> rootArray.booleanType(BOOLEAN_EXAMPLE)
            ZonedDateTime::class -> rootArray.datetimeExpression(DATETIME_EXPRESSION_EXAMPLE, ISO_PATTERN)
            else -> {
                rootArray.`object` { objDsl ->
                    objDsl.run {
                        if (!alreadyProcessedObject.contains(listTypeCLass)) {
                            fillBasedOnConstructorFields(
                                listTypeCLass?.primaryConstructor,
                                objDsl,
                                alreadyProcessedObject
                            )
                        }
                    }
                }
            }
        }
    }
}
