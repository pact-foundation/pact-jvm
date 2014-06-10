package au.com.dius.pact

package object consumer {
  type ConsumerTestVerification[T] = T => Option[T]
}
