package au.com.dius.pact.model

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
      val mergedInteractions = interactions ++ otherInteractions.filterNot(interactions.contains)
      MergeSuccess(new Pact(existing.getProvider, existing.getConsumer, JavaConversions.seqAsJavaList(mergedInteractions)))
    } else {
      MergeConflict(failures)
    }
  }

  //
  //  def interactionFor(description:String, providerState: Option[String]) = interactions.find { i =>
  //    i.description == description && i.providerState == providerState
  //  }

}
