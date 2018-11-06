package io.pactfoundation.consumer.dsl

fun LambdaDslJsonArray.newObject(o: (LambdaDslObject) -> (Unit)): LambdaDslJsonArray {
  return this.`object`(o)
}
