package au.com.dius.pact.support.expressions

interface ValueResolver {
  fun resolveValue(expression: String): String
  fun propertyDefined(property: String): Boolean
}
