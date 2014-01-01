package com.dius.pact.model.waitingforscalaz

object Align {
  def apply[A, B](a:List[A], b:List[B]):List[These[A, B]] = {
    val size = Math.max(a.length, b.length)
    padTo(a, size).zip(padTo(b, size)).map {
      case (Some(aa), Some(bb)) => Both(aa, bb)
      case (Some(aa), None) => This[A, B](aa)
      case (None, Some(bb)) => That[A, B](bb)
    }
  }

  private def padTo[T](a:List[T], length:Int):List[Option[T]] = {
    a.map(Some[T]).padTo(length, None)
  }
}
