package au.com.dius.pact.consumer.junit5

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy


// Shorthand for @ExtendWith(PactConsumerTestExt::class)
@ExtendWith(PactConsumerTestExt::class)
@Retention(RetentionPolicy.RUNTIME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS
)
annotation class PactConsumerTest
