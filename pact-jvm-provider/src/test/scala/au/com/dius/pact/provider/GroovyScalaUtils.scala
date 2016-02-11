package au.com.dius.pact.provider

import _root_.org.apache.http.HttpRequest

object GroovyScalaUtils {

  def testRequestFilter = (httpRequest: HttpRequest) => httpRequest.addHeader("Scala", "Was Called")

}
