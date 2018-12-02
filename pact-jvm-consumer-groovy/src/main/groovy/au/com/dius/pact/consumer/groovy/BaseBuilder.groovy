package au.com.dius.pact.consumer.groovy

/**
 * Base class for builders
 */
class BaseBuilder extends Matchers {
  public static final List COMPACT_MIME_TYPES = ['application/x-thrift+json']

  def call(Closure closure) {
    build(closure)
  }

  def build(Closure closure) {
    callWithThisAsDelegate(closure)
  }

  def callWithThisAsDelegate(Closure closure) {
    closure.delegate = this
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.call()
  }

}
