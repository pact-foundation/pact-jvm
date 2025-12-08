package au.com.dius.pact.provider.spring.spring7

import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import org.springframework.core.env.Environment

class Spring7EnvironmentResolver(private val environment: Environment) : ValueResolver {
    override fun resolveValue(property: String?): String? {
        val tuple = SystemPropertyResolver.PropertyValueTuple(property).invoke()

        val name = tuple.propertyName ?: return null
        val defaultValue = tuple.defaultValue ?: return null

        return environment.getProperty(name, defaultValue)
    }

    override fun resolveValue(property: String?, default: String?): String? {
        val name = property ?: return null
        val defaultValue = default ?: return null
        return environment.getProperty(name, defaultValue)
    }

  override fun propertyDefined(property: String) = environment.containsProperty(property)
}
