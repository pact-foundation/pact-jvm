package au.com.dius.pact.consumer.junit5

import org.junit.jupiter.api.extension.ExtendWith

// Shorthand for @ExtendWith(PactConsumerTestExt::class)
@ExtendWith(PactConsumerTestExt::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS
)
annotation class PactConsumerTest
