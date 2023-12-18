package au.com.dius.pact.core.model.annotations

/**
 * describes the interactions between a provider and a consumer used in JUnit tests.
 * The annotated method has to be one of following signatures:
 *
 * For legacy DSL classes and request/response interactions:
 * public RequestResponsePact providerDef1(PactDslWithProvider builder) {...}
 *
 * For message interactions:
 * public MessagePact providerDef1(MessagePactBuilder builder)
 *
 * For V4 DSL classes and any interaction type:
 * public V4Pact providerDef1(PactBuilder builder) {...}
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
