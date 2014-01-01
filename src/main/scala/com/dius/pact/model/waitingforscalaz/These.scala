package com.dius.pact.model.waitingforscalaz

/**
 * scalaz 2.1.0 has a much better implementation of this structure in scalaz.\&/.These but it's not yet released and compatible with specs2
 */
sealed trait These[A, B]

case class This[A, B](a:A) extends These[A, B]
case class That[A, B](b:B) extends These[A, B]
case class Both[A, B](a:A, b:B) extends These[A, B]