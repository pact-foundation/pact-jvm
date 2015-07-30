package au.com.dius.pact.consumer.groovy

@SuppressWarnings('UnusedImport')
import au.com.dius.pact.consumer.PactVerified$

/**
 * Base class for builders
 */
class BaseBuilder extends Matchers {

    public static final PactVerified$ PACTVERIFIED = PactVerified$.MODULE$

    def call(Closure closure) {
        build(closure)
    }

    def build(Closure closure) {
        closure.delegate = this
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
    }

}
