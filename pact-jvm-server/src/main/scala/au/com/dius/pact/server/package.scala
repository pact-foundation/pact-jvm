package au.com.dius.pact

import au.com.dius.pact.consumer.StatefulMockProvider
import au.com.dius.pact.model.RequestResponseInteraction

package object server {
  type ServerState = Map[Int, StatefulMockProvider[RequestResponseInteraction]]
}
