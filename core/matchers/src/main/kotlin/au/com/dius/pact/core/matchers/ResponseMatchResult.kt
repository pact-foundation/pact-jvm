package au.com.dius.pact.core.matchers

data class ResponseMatchResult(
  val status: Mismatch?,
  val headers: List<HeaderMatchResult>,
  val body: BodyMatchResult
) {
  val mismatches: List<Mismatch>
    get() {
      val list = mutableListOf<Mismatch>()
      if (status != null) {
        list.add(status)
      }
      list.addAll(headers.flatMap { it.result })
      list.addAll(body.mismatches)
      return list
    }

  fun matchedOk() = status == null && headers.all { it.result.isEmpty() } && body.matchedOk()
}
