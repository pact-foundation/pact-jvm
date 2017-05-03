package au.com.dius.pact.consumer

import au.com.dius.pact.model.RequestPartMismatch
import scala.Option
import scala.collection.JavaConversions
import scala.collection.Seq

object ScalaCollectionUtils {
  fun toList(mismatches: Option<Seq<RequestPartMismatch>>?): List<RequestPartMismatch> {
    return if (mismatches != null && mismatches.isDefined) {
      JavaConversions.seqAsJavaList(mismatches.get())
    } else {
      listOf()
    }
  }
}
