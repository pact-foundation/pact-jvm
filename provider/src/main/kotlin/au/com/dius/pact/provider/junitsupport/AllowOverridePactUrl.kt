package au.com.dius.pact.provider.junitsupport

import java.lang.annotation.Inherited

/**
 * This will mark the test to use any pact URL from the Java system properties `pact.filter.pacturl` and either the
 * `pact.filter.consumers` system property or the @Consumer annotation.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Inherited
annotation class AllowOverridePactUrl
