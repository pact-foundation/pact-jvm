/**
 * This file inlined from https://github.com/kittinunf/Result
 */
package au.com.dius.pact.core.pactbroker.com.github.kittinunf.result

class Validation<out E : Exception>(vararg resultSequence: Result<*, E>) {

    val failures: List<E> = resultSequence.filterIsInstance<Result.Failure<*, E>>().map { it.getException() }

    val hasFailure = failures.isNotEmpty()
}
