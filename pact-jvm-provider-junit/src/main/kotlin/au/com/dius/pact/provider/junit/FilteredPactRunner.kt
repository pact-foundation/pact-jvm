package au.com.dius.pact.provider.junit

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.junit.loader.PactFilter
import java.util.function.Predicate

/**
 * Pact Runner that uses annotations to filter the interactions that are executed
 */
open class FilteredPactRunner(clazz: Class<*>) : PactRunner(clazz) {

  public override fun filterPacts(pacts: List<Pact>): List<Pact> {
    val pactFilterValues = this.testClass.javaClass.getAnnotation(PactFilter::class.java)?.value
    return if (pactFilterValues != null && pactFilterValues.filter { !it.isNullOrEmpty() }.isNotEmpty()) {
      pacts.map { pact ->
        pact.filterInteractions(Predicate<Interaction> { interaction ->
          pactFilterValues.filter { value -> interaction.providerStates.any { it.matches(value) } }.isNotEmpty()
        })
      }.filter { pact -> pact.interactions.isNotEmpty() }
    } else pacts
  }
}
