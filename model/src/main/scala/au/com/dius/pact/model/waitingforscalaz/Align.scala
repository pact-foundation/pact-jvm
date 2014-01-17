package au.com.dius.pact.model.waitingforscalaz

import scalaz._
import Scalaz._

object Align {
  def apply[A, B](thises:List[A], thats:List[B]):List[These[A, B]] = {
    @annotation.tailrec
    def loop(aa: List[A], bb: List[B], accum: List[These[A, B]]): List[These[A, B]] = (aa, bb) match {
      case (Nil, _) =>
        accum reverse_::: bb.map(b => That[A, B](b))
      case (_, Nil) =>
        accum reverse_::: aa.map(a => This[A, B](a))
      case (ah :: at, bh :: bt) =>
        loop(at, bt, Both(ah, bh) :: accum)
    }
    loop(thises, thats, Nil)
  }
}
