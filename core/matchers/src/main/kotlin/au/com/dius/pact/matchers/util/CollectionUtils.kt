package au.com.dius.pact.matchers.util

fun tails(col: List<String>): List<List<String>> {
  val result = mutableListOf<List<String>>()
  var acc = col
  while (acc.isNotEmpty()) {
    result.add(acc)
    acc = acc.drop(1)
  }
  result.add(acc)
  return result
}

fun <A, B> corresponds(l1: List<A>, l2: List<B>, fn: (a: A, b: B) -> Boolean): Boolean {
  return if (l1.size == l2.size) {
    l1.zip(l2).all { fn(it.first, it.second) }
  } else {
    false
  }
}
