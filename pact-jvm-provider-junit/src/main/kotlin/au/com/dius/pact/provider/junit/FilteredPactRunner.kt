package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.provider.junit.loader.PactFilter
import java.util.function.Predicate

/**
 * Pact Runner that uses annotations to filter the interactions that are executed
 */
@Deprecated("This functionality has been moved to the base PactRunner (will be removed in 4.0.x)")
open class FilteredPactRunner<I>(clazz: Class<*>) : PactRunner<I>(clazz) where I : Interaction {
  public override fun filterPacts(pacts: List<Pact<I>>): List<Pact<I>> {
    val pactFilterValues = this.testClass.javaClass.getAnnotation(PactFilter::class.java)?.value
    return if (pactFilterValues != null && pactFilterValues.any { !it.isEmpty() }) {
      pacts.map { pact ->
        FilteredPact(pact, Predicate { interaction ->
          pactFilterValues.any { value -> interaction.providerStates.any { it.matches(value) } }
        })
      }.filter { pact -> pact.interactions.isNotEmpty() }
    } else pacts
  }
}
