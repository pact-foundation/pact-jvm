package au.com.dius.pact.consumer.spock

import org.spockframework.runtime.extension.ExtensionAnnotation

/**
 * Activates the Pact consumer extension for a Spock spec. Apply to the spec class.
 *
 * Usage:
 * ```groovy
 * @PactConsumerSpockTest
 * class MyConsumerSpec extends Specification {
 *
 *     MockServer mockServer   // injected by the extension
 *
 *     @Pact(provider = 'MyProvider')
 *     RequestResponsePact myPact(PactDslWithProvider builder) { ... }
 *
 *     @PactTestFor(pactMethod = 'myPact')
 *     def 'calls the provider'() { ... }
 * }
 * ```
 */
@ExtensionAnnotation(PactConsumerSpockExt::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class PactConsumerSpockTest
