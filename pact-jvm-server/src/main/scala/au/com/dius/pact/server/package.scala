package au.com.dius.pact

package object server {
  type ServerState = Map[String, StatefulMockProvider]
}
