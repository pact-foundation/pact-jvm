package au.com.dius.pact

import au.com.dius.pact.consumer.MockServiceProvider

package object server {
  type ServerState = Map[Int, MockServiceProvider]
}
