package au.com.dius.pact.core.model.annotations

/**
 * describes the interactions between a provider and a consumer used in JUnit tests.
 * The annotated method has to be of following signature:
 *
 * public RequestResponsePact providerDef1(PactDslWithProvider builder) {...}
 *
 * @author pmucha
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Pact(
  /**
   * name of the provider
   */
  val provider: String = "",

  /**
   * name of the consumer
   */
  val consumer: String
)
