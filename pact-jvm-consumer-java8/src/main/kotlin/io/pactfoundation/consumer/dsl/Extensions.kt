package io.pactfoundation.consumer.dsl

import au.com.dius.pact.consumer.dsl.DslPart

/**
 * DSL function to simplify creating a [DslPart] generated from a [LambdaDslJsonBody].
 */
fun newJsonObject(body: LambdaDslJsonBody.() -> Unit): DslPart {
  return LambdaDsl.newJsonBody { it.body() }.build()
}

/**
 * DSL function to simplify creating a [DslPart] generated from a [LambdaDslJsonArray].
 */
fun newJsonArray(body: LambdaDslJsonArray.() -> Unit): DslPart {
  return LambdaDsl.newJsonArray { it.body() }.build()
}

/**
 * Extension function to make [LambdaDslObject.object] Kotlin friendly.
 */
fun LambdaDslObject.newObject(name: String, nestedObject: LambdaDslObject.() -> Unit): LambdaDslObject {
  return `object`(name) { it.nestedObject() }
}

/**
 * Extension function to make [LambdaDslObject.array] Kotlin friendly.
 */
fun LambdaDslObject.newArray(name: String, body: LambdaDslJsonArray.() -> (Unit)): LambdaDslObject {
  return array(name) { it.body() }
}

/**
 * Extension function to make [LambdaDslJsonArray.array] Kotlin friendly.
 */
fun LambdaDslJsonArray.newArray(body: LambdaDslJsonArray.() -> (Unit)): LambdaDslJsonArray {
  return array { it.body() }
}

/**
 * Extension function to make [LambdaDslJsonArray.array] Kotlin friendly.
 */
fun LambdaDslJsonArray.newObject(body: LambdaDslObject.() -> (Unit)): LambdaDslJsonArray {
  return `object` { it.body() }
}
