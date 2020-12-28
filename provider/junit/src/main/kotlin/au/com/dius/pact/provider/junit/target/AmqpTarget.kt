package au.com.dius.pact.provider.junit.target

import au.com.dius.pact.core.model.Interaction

/**
 * Out-of-the-box implementation of [Target], that run [Interaction] against message pact and verify response
 * By default it will scan all packages for annotated methods, but a list of packages can be provided to reduce
 * the performance cost
 * @param packagesToScan List of JVM packages
 */
@Deprecated("Use MessageTarget")
open class AmqpTarget @JvmOverloads constructor(
  packagesToScan: List<String> = emptyList(),
  classLoader: ClassLoader? = null
) : MessageTarget(packagesToScan, classLoader)
