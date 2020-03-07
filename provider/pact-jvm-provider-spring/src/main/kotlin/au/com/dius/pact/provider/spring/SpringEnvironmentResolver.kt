package au.com.dius.pact.provider.spring

import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.core.support.isNotEmpty
import org.springframework.core.env.Environment

class SpringEnvironmentResolver(private val environment: Environment) : ValueResolver {
  override fun resolveValue(property: String?): String? {
    return if (property.isNotEmpty()) environment.getProperty(property) else null
  }

  override fun propertyDefined(property: String) = environment.containsProperty(property)
}
