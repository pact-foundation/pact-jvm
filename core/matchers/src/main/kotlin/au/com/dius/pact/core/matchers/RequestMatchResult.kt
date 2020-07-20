package au.com.dius.pact.core.matchers

data class RequestMatchResult(
  val method: MethodMismatch?,
  val path: PathMismatch?,
  val query: List<QueryMatchResult>,
  val cookie: CookieMismatch?,
  val headers: List<HeaderMatchResult>,
  val body: BodyMatchResult
) {
  val mismatches: List<Mismatch>
    get() {
      val list = mutableListOf<Mismatch>()
      if (method != null) {
        list.add(method)
      }
      if (path != null) {
        list.add(path)
      }
      list.addAll(query.flatMap { it.result })
      if (cookie != null) {
        list.add(cookie)
      }
      list.addAll(headers.flatMap { it.result })
      list.addAll(body.mismatches)
      return list
    }

  fun matchedOk() = method == null && path == null && query.all { it.result.isEmpty() } && cookie == null &&
    headers.all { it.result.isEmpty() } && body.matchedOk()

  fun matchedMethodAndPath() = method == null && path == null

  fun calculateScore(): Int {
    var score = 0
    if (method == null) {
      score += 1
    } else {
      score -= 1
    }
    if (path == null) {
      score += 1
    } else {
      score -= 1
    }
    query.forEach {
      if (it.result.isEmpty()) {
        score += 1
      } else {
        score -= 1
      }
    }
    if (cookie == null) {
      score += 1
    } else {
      score -= 1
    }
    headers.forEach {
      if (it.result.isEmpty()) {
        score += 1
      } else {
        score -= 1
      }
    }
    if (body.typeMismatch != null) {
      score -= 1
    } else {
      body.bodyResults.forEach {
        if (it.result.isEmpty()) {
          score += 1
        } else {
          score -= 1
        }
      }
    }
    return score
  }
}
