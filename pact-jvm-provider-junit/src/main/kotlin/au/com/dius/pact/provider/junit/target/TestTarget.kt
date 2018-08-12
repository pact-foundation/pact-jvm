package au.com.dius.pact.provider.junit.target

import java.lang.annotation.Inherited

/**
 * Mark [au.com.dius.pact.provider.junit.target.Target] for contract tests
 *
 * @see au.com.dius.pact.provider.junit.target.Target
 *
 * @see HttpTarget
 */
@Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FIELD)
@Inherited
annotation class TestTarget
