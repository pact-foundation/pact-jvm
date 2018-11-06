package au.com.dius.pact.consumer.dsl

object Dsl {
  /**
   * Creates a builder to define the matchers on an array of JSON primitives
   */
  @JvmStatic
  fun arrayOfPrimitives() = ArrayOfPrimitivesBuilder()
}
