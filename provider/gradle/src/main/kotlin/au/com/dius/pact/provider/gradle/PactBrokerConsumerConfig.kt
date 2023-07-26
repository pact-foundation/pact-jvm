package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelectors
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder
import io.github.oshai.kotlinlogging.KLogging
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Config for pact broker
 */
open class PactBrokerConsumerConfig @Inject constructor(
  val objectFactory: ObjectFactory
) {
  var selectors: MutableList<ConsumerVersionSelectors>? = mutableListOf()
  var enablePending: Boolean? = false
  var providerTags: List<String>? = listOf()
  var providerBranch: String? = ""

  /**
   * DSL to configure the consumer version selectors
   */
  fun withSelectors(action: Action<ConsumerVersionSelectorConfig>) {
    val config = objectFactory.newInstance(ConsumerVersionSelectorConfig::class.java)

    action.execute(config)

    if (selectors == null) {
      selectors = mutableListOf()
    }
    selectors!!.addAll(config.selectors)
  }

  companion object : KLogging() {
    @JvmStatic
    @JvmOverloads
    @Deprecated(message = "Assigning selectors with latestTags is deprecated, use withSelectors instead")
    fun latestTags(options: Map<String, Any> = mapOf(), vararg tags: String): List<ConsumerVersionSelectors> {
      logger.warn { "Assigning selectors with latestTags is deprecated, use withSelectors instead" }
      return tags.map {
        ConsumerVersionSelectors.Selector(it, true, null, options["fallbackTag"]?.toString())
      }
    }
  }
}

open class ConsumerVersionSelectorConfig: SelectorBuilder()
