package au.com.dius.pact

import au.com.dius.pact.consumer.MockProvider
import au.com.dius.pact.consumer.StatefulMockProvider

package object server {
  type ServerState = Map[Int, StatefulMockProvider]
}
