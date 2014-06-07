package au.com.dius.pact

import scala.util.Try

package object consumer {
  type ConsumerTestVerification[T] = T => Try[T]
}
