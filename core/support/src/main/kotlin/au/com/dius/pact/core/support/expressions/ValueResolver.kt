package au.com.dius.pact.core.support.expressions

interface ValueResolver {
  fun resolveValue(property: String?): String?
  fun resolveValue(property: String?, default: String?): String?
  fun propertyDefined(property: String): Boolean
}

data class MapValueResolver(val context: Map<String, Any?>) : ValueResolver {
  override fun resolveValue(property: String?) = context[property ?: ""]?.toString()
  override fun resolveValue(property: String?, default: String?) = resolveValue(property) ?: default

  override fun propertyDefined(property: String) = context.containsKey(property)
}
