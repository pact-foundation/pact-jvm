package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelectors
import mu.KLogging
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

open class ConsumerVersionSelectorConfig {
  val selectors: MutableList<ConsumerVersionSelectors> = mutableListOf()

  /**
   * The latest version from the main branch of each consumer, as specified by the consumer's mainBranch property.
   */
  fun mainBranch() {
    selectors.add(ConsumerVersionSelectors.MainBranch)
  }

  /**
   * The latest version from a particular branch of each consumer, or for a particular consumer if the second
   * parameter is provided. If fallback is provided, falling back to the fallback branch if none is found from the
   * specified branch.
   *
   * @param name - Branch name
   * @param consumer - Consumer name (optional)
   * @param fallback - Fall back to this branch if none is found from the specified branch (optional)
   */
  @JvmOverloads
  fun branch(name: String, consumer: String? = null, fallback: String? = null) {
    selectors.add(ConsumerVersionSelectors.Branch(name, consumer, fallback))
  }

  /**
   * All the currently deployed and currently released and supported versions of each consumer.
   */
  fun deployedOrReleased() {
    selectors.add(ConsumerVersionSelectors.DeployedOrReleased)
  }

  /**
   * The latest version from any branch of the consumer that has the same name as the current branch of the provider.
   * Used for coordinated development between consumer and provider teams using matching feature branch names.
   */
  fun matchingBranch() {
    selectors.add(ConsumerVersionSelectors.MatchingBranch)
  }

  /**
   * Any versions currently deployed to the specified environment
  */
  fun deployedTo(environment: String) {
    selectors.add(ConsumerVersionSelectors.DeployedTo(environment))
  }

  /**
   * Any versions currently released and supported in the specified environment
   */
  fun releasedTo(environment: String) {
    selectors.add(ConsumerVersionSelectors.ReleasedTo(environment))
  }

  /**
   * Any versions currently released and supported in the specified environment
   */
  fun environment(environment: String) {
    selectors.add(ConsumerVersionSelectors.Environment(environment))
  }
}
