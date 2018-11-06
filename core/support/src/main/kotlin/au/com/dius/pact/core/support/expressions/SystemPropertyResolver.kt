package au.com.dius.pact.core.support.expressions

import org.apache.commons.lang3.StringUtils
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.contains

class SystemPropertyResolver : ValueResolver {

  override fun resolveValue(property: String?): String? {
    val tuple = PropertyValueTuple(property).invoke()
    return if (property.isNotEmpty()) {
      var propertyValue = System.getProperty(tuple.propertyName!!)
      if (propertyValue == null) {
        propertyValue = System.getenv(tuple.propertyName)
      }
      if (propertyValue == null) {
        propertyValue = tuple.defaultValue
      }
      if (propertyValue == null) {
        throw RuntimeException("Could not resolve property \"${tuple.propertyName}\" in the system properties or " +
          "environment variables and no default value is supplied")
      }
      propertyValue
    } else {
      property
    }
  }

  override fun propertyDefined(property: String): Boolean {
    var propertyValue: String? = System.getProperty(property)
    if (propertyValue == null) {
      propertyValue = System.getenv(property)
    }
    return propertyValue != null
  }

  class PropertyValueTuple(property: String?) {
    var propertyName: String? = null
      private set
    var defaultValue: String? = null
      private set

    init {
      this.propertyName = property
      this.defaultValue = null
    }

    operator fun invoke(): PropertyValueTuple {
      if (propertyName.contains(":")) {
        val kv = StringUtils.splitPreserveAllTokens(propertyName, ':')
        propertyName = kv[0]
        if (kv.size > 1) {
          defaultValue = kv[1]
        }
      }
      return this
    }
  }
}
