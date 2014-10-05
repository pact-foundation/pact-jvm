package au.com.dius.pact.consumer.groovy

class PactBodyBuilder {

  def bodyMap = [:]
  def matchers = [:]
  def path = '$.body'

  def call(Closure closure) {
    build(closure)
  }

  def build(Closure closure) {
    closure.delegate = this
    closure.call()
  }

  String getBody() {

  }

  def methodMissing(String name, args) {
    println "$name -> $args"

    switch(name) {
      case 'regexp':
        if (args.size() > 0) {

        } else {
          matchers[path + '.' + name] = 
        }
        break
      default:
        bodyMap[name] = args
    }
  }

  def propertyMissing(String name) {
    println ">> $name"
  }

}
