package au.com.dius.pact.provider.groovysupport

import org.apache.http.HttpRequest

object GroovyScalaUtils {

  def testRequestFilter = (httpRequest: HttpRequest) => httpRequest.addHeader("Scala", "Was Called")

}
