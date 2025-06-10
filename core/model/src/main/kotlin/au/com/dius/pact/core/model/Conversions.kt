package au.com.dius.pact.core.model

fun interface Into<N> {
  fun into(): N
}

// stupid detekt
@Suppress("EmptyClassBlock")
object Conversions {}
