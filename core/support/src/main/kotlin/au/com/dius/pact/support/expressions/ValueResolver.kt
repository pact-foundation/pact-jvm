package au.com.dius.pact.support.expressions

interface ValueResolver {
  fun resolveValue(property: String?): String?
  fun propertyDefined(property: String): Boolean
}

data class MapValueResolver(val context: Map<String, Any?>) : ValueResolver {
  override fun resolveValue(property: String?) = context[property ?: ""].toString()

  override fun propertyDefined(property: String) = context.containsKey(property)
}
