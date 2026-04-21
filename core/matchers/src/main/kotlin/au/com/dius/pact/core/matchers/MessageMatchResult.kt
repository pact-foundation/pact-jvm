package au.com.dius.pact.core.matchers

data class MetadataMatchResult(val key: String, val result: List<MetadataMismatch>)

data class MessageMatchResult(
  val contents: BodyMatchResult,
  val metadata: List<MetadataMatchResult>
) {
  val mismatches: List<Mismatch>
    get() {
      val list = mutableListOf<Mismatch>()
      list.addAll(contents.mismatches)
      list.addAll(metadata.flatMap { it.result })
      return list
    }

  fun matchedOk() = contents.matchedOk() && metadata.all { it.result.isEmpty() }
}
