package au.com.dius.pact.provider.scalatest

import java.net.URL

/**
  * This trait provides a link between your server implementation and scalatest
  */
trait ServerStarter {

  /**
    * method to start the underlying server implementation
    *
    * @return URL for the server
    */
  def startServer(): URL

  /**
    * This method is called before each and every interaction test
    *
    * @param state is the 'provider_state' attribute from the pact
    */
  def initState(state: String): Unit


  /**
    * method to stop the underlying server implementation
    */
  def stopServer(): Unit
}
