package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.junit.filter.InteractionFilter
import au.com.dius.pact.provider.junit.loader.OverrideablePactLoader
import au.com.dius.pact.provider.junit.loader.PactFilter
import au.com.dius.pact.provider.junit.loader.PactLoader
import mu.KLogging
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import kotlin.reflect.full.createInstance

object JUnitProviderTestSupport : KLogging() {
  fun <I> filterPactsByAnnotations(pacts: List<Pact<I>>, testClass: Class<*>): List<Pact<I>> where I : Interaction {
    val pactFilter = testClass.getAnnotation(PactFilter::class.java) ?: return pacts
    if (pactFilter.value.all { it.isEmpty() }) return pacts

    val interactionFilter = pactFilter.filter.createInstance() as InteractionFilter<I>
    return pacts.map { pact ->
      FilteredPact(pact, interactionFilter.buildPredicate(pactFilter.value))
    }.filter { pact -> pact.interactions.isNotEmpty() }
  }

  @JvmStatic
  fun generateErrorStringFromMismatches(mismatches: Map<String, Any>): String {
    return System.lineSeparator() + mismatches.values
      .mapIndexed { i, value ->
        val errPrefix = "$i - "
        when (value) {
          is Throwable -> errPrefix + exceptionMessage(value, errPrefix.length)
          is Map<*, *> -> errPrefix + convertMapToErrorString(value as Map<String, *>)
          else -> errPrefix + value.toString()
        }
      }.joinToString(System.lineSeparator())
  }

  @JvmStatic
  fun exceptionMessage(err: Throwable, prefixLength: Int): String {
    val message = err.message

    val cause = err.cause
    var details = ""
    if (cause != null) {
      details = ExceptionUtils.getStackTrace(cause)
    }

    val lineSeparator = System.lineSeparator()
    return if (message != null && message.contains("\n")) {
      val padString = StringUtils.leftPad("", prefixLength)
      val lines = message.split("\n")
      lines.reduceIndexed { index, acc, line ->
        if (index > 0) {
          acc + lineSeparator + padString + line
        } else {
          line + lineSeparator
        }
      }
    } else {
      "$message\n$details"
    }
  }

  private fun convertMapToErrorString(mismatches: Map<String, *>): String {
    return if (mismatches.containsKey("comparison")) {
      val comparison = mismatches["comparison"]
      if (mismatches.containsKey("diff")) {
        mapToString(comparison as Map<String, *>)
      } else {
        if (comparison is Map<*, *>) {
          mapToString(comparison as Map<String, *>)
        } else {
          comparison.toString()
        }
      }
    } else {
      mapToString(mismatches)
    }
  }

  private fun mapToString(comparison: Map<String, *>): String {
    return comparison.entries.joinToString(System.lineSeparator()) { (key, value) -> "$key -> $value" }
  }

  @JvmStatic
  fun checkForOverriddenPactUrl(
    loader: PactLoader?,
    overridePactUrl: AllowOverridePactUrl?,
    consumer: Consumer?
  ) {
    var pactUrl = System.getProperty(ProviderVerifier.PACT_FILTER_PACTURL)
    if (pactUrl.isNullOrEmpty()) {
      pactUrl = System.getenv(ProviderVerifier.PACT_FILTER_PACTURL)
    }

    if (loader is OverrideablePactLoader && overridePactUrl != null && pactUrl.isNotEmpty()) {
      var consumerProperty = System.getProperty(ProviderVerifier.PACT_FILTER_CONSUMERS)
      if (consumerProperty.isNullOrEmpty()) {
        consumerProperty = System.getenv(ProviderVerifier.PACT_FILTER_CONSUMERS)
      }
      when {
        consumerProperty.isNotEmpty() -> loader.overridePactUrl(pactUrl, consumerProperty)
        consumer != null -> loader.overridePactUrl(pactUrl, consumer.value)
        else -> {
          logger.warn {
            "The property ${ProviderVerifier.PACT_FILTER_PACTURL} has been set, but no consumer filter" +
              " or @Consumer annotation has been provided, Ignoring"
          }
        }
      }
    }
  }
}
