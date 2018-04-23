package au.com.dius.pact.provider.junit

import au.com.dius.pact.model.FilteredPact
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.junit.loader.PactFilter
import java.util.function.Predicate

object JUnitProviderTestSupport {
  fun <I> filterPactsByAnnotations(pacts: List<Pact<I>>, testClass: Class<*>): List<Pact<I>> where I: Interaction {
    val pactFilterValues = testClass.getAnnotation(PactFilter::class.java)?.value
    return if (pactFilterValues != null && pactFilterValues.any { !it.isEmpty() }) {
      pacts.map { pact ->
        FilteredPact(pact, Predicate { interaction ->
          pactFilterValues.any { value -> interaction.providerStates.any { it.matches(value) } }
        })
      }.filter { pact -> pact.interactions.isNotEmpty() }
    } else pacts
  }
}
