package au.com.dius.pact.consumer.groovy

/**
 * Base class for builders
 */
class GroovyBuilder extends BaseBuilder {
  def call(Closure closure) {
    build(closure)
  }

  def build(Closure closure) {
    closure.delegate = this
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.call()
  }
}
