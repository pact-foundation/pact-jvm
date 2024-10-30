package au.com.dius.pact.server

import au.com.dius.pact.core.matchers.PartialRequestMatch
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Request
import scala.collection.JavaConverters.asJavaIterable

data class PactSessionResults(
  val matched: List<Interaction>,
  val almostMatched: List<PartialRequestMatch>,
  val missing: List<Interaction>,
  val unexpected: List<Request>
) {
  fun addMatched(inter: Interaction) = copy(matched = listOf(inter) + matched)
  fun addUnexpected(request: Request) = copy(unexpected = listOf(request) + unexpected)
  fun addMissing(inters: scala.collection.Iterable<Interaction>) = copy(missing = asJavaIterable(inters).toList() + missing)
  fun addAlmostMatched(partial: PartialRequestMatch) = copy(almostMatched = listOf(partial) + almostMatched)

  fun allMatched(): Boolean = missing.isEmpty() && unexpected.isEmpty()

  companion object {
    @JvmStatic
    val empty = PactSessionResults(emptyList(), emptyList(), emptyList(), emptyList())
  }
}
