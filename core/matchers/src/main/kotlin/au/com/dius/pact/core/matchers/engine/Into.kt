package au.com.dius.pact.core.matchers.engine

fun interface Into<N> {
  fun into(): N
}

typealias StrInto = Into<String>
