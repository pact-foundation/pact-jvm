package au.com.dius.pact.model

import au.com.dius.pact.model.v3.messaging.MessagePact

import scala.collection.JavaConversions

trait MergeResult

case class MergeSuccess(result: Pact) extends MergeResult
case class MergeConflict(result: Seq[(Interaction, Interaction)]) extends MergeResult

object PactMerge {
  def merge(newPact: Pact, existing: Pact) : MergeResult = {
    val interactions = JavaConversions.collectionAsScalaIterable(existing.getInteractions).toSeq
    val otherInteractions = JavaConversions.collectionAsScalaIterable(newPact.getInteractions).toSeq
    val failures: Seq[(Interaction, Interaction)] = for {
      a <- interactions
      b <- otherInteractions
      if a conflictsWith b
    } yield (a, b)

    if (failures.isEmpty) {
      val result = existing match {
        case reqRes: RequestResponsePact =>
          val mergedInteractions = interactions ++ otherInteractions.filterNot(interactions.contains)
          new RequestResponsePact(existing.getProvider, existing.getConsumer, JavaConversions.seqAsJavaList(mergedInteractions).asInstanceOf[java.util.List[RequestResponseInteraction]])
        case messagePact: MessagePact =>
          messagePact.mergePact(newPact)
      }
      result.sortInteractions()
      MergeSuccess(result)
    } else {
      MergeConflict(failures)
    }
  }
}
