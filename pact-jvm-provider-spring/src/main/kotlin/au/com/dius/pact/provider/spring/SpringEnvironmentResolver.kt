package au.com.dius.pact.provider.spring

import au.com.dius.pact.provider.junit.sysprops.SystemPropertyResolver
import au.com.dius.pact.provider.junit.sysprops.ValueResolver
import org.springframework.core.env.Environment

class SpringEnvironmentResolver(private val environment: Environment) : ValueResolver {
  override fun resolveValue(expression: String): String {
    val tuple = SystemPropertyResolver.PropertyValueTuple(expression).invoke()
    return environment.getProperty(tuple.propertyName, tuple.defaultValue)
  }

  override fun propertyDefined(property: String) = environment.containsProperty(property)
}
