package au.com.dius.pact.consumer.junit5

/**
 * Marks a injected MockServer parameter as for a particular provider. This is used when there is more than one
 * provider setup for the test.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ForProvider(val value: String)

/**
 * Marks a test as a non-pact test. This will cause the normal Pact lifecycle to be skipped for that test.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class PactIgnore
