package au.com.dius.pact.provider.spring.spring6

import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import org.springframework.core.env.Environment

class Spring6EnvironmentResolver(private val environment: Environment) : ValueResolver {
  override fun resolveValue(property: String?): String? {
    val tuple = SystemPropertyResolver.PropertyValueTuple(property).invoke()
    return if (tuple.propertyName != null) {
      environment.getProperty(tuple.propertyName!!, tuple.defaultValue.orEmpty())
    } else {
      null
    }
  }

  override fun resolveValue(property: String?, default: String?): String? {
    return if (property != null) {
      environment.getProperty(property, default.orEmpty())
    } else {
      null
    }
  }

  override fun propertyDefined(property: String) = environment.containsProperty(property)
}
