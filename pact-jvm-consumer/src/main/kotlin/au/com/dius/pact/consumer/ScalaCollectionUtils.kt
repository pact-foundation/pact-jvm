package au.com.dius.pact.consumer

import au.com.dius.pact.matchers.Mismatch
import scala.Option
import scala.collection.JavaConversions
import scala.collection.Seq

object ScalaCollectionUtils {
  fun toList(mismatches: Option<Seq<Mismatch>>?): List<Mismatch> {
    return if (mismatches != null && mismatches.isDefined) {
      JavaConversions.seqAsJavaList(mismatches.get())
    } else {
      listOf()
    }
  }
}
