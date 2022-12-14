package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.PactSpecVersion
import groovy.lang.Closure

/**
 * Base class for Groovy based builders
 */
open class GroovyBuilder(pactVersion: PactSpecVersion): BaseBuilder(pactVersion) {
  open fun call(closure: Closure<Any?>) = build(closure)

  open fun build(closure: Closure<Any?>): Any? {
    closure.delegate = this
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    return closure.call()
  }
}
