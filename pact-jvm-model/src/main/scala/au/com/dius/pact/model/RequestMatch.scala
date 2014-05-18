package au.com.dius.pact.model

sealed trait RequestMatch extends Ordered[RequestMatch] {
  def allMatched = false
  
  protected def goodness: Int
  def compare(that: RequestMatch): Int = goodness.compare(that.goodness)
  
  def toOption: Option[Interaction] = this match {
    case FullRequestMatch(inter) => Some(inter)
    case _ => None
  }

  /**
   * Take the first total match, or merge partial matches, or take the best available.
   */
  def merge(other: RequestMatch): RequestMatch = (this, other) match {
    case (a @ FullRequestMatch(_), FullRequestMatch(_)) => a
    case (a @ PartialRequestMatch(problems1), 
          b @ PartialRequestMatch(problems2)) => PartialRequestMatch(a.problems ++ b.problems)
    case (a, b) =>  if (a > b) a else b 
  }
}

case class FullRequestMatch(interaction: Interaction) extends RequestMatch { 
  override def allMatched = true 
  override protected def goodness = 2
}

object PartialRequestMatch {
  def apply(expected: Interaction, mismatches: Seq[RequestPartMismatch]): PartialRequestMatch =
    PartialRequestMatch(Map(expected -> mismatches))
}

case class PartialRequestMatch(problems: Map[Interaction, Seq[RequestPartMismatch]]) extends RequestMatch {
  // These invariants should be enforced by a better use of the type system. NonEmptyList, etc
  require(problems.nonEmpty, "Partial match must contain some failed matches")
  require(problems.values.forall(_.nonEmpty), "Mismatch lists shouldn't be empty")
  
  override protected def goodness = 1
}

case object RequestMismatch extends RequestMatch {
  override protected def goodness = 0
}