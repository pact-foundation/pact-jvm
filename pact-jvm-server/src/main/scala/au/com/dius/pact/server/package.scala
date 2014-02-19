package au.com.dius.pact

import au.com.dius.pact.consumer.MockServiceProvider.StartedMockServiceProvider

package object server {
  type ServerState = Map[Int, StartedMockServiceProvider]

  val noHeaders = Map[String, String]()

  val crossSiteHeaders = Map[String, String]("Access-Control-Allow-Origin" -> "*")
}
