package au.com.dius.pact.sbt

sealed trait GitResult[+A] {
  def msg: String

  def flatMap[B](f: A => GitResult[B]): GitResult[B] = this match {
    case HappyResult(successMsg) => f(successMsg)
    case self: FailedResult => self
    case self: TerminatingResult => self
  }

  def map[B](f: A => B): GitResult[B] = this match {
    case HappyResult(successMsg) => HappyResult(f(successMsg))
    case self: FailedResult => self
    case self: TerminatingResult => self
  }

  def verifyResult(): Unit = this match {
    case FailedResult(msg) => sys.error(msg)
    case _ => // All good
  }
}

final case class HappyResult[+A](successMsg: A) extends GitResult[A] {
  def msg = successMsg.toString
}

final case class FailedResult(msg: String) extends GitResult[Nothing]

final case class TerminatingResult(msg: String) extends GitResult[Nothing]