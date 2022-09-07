package au.com.dius.pact.provider.junitsupport.loader

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelectors
import au.com.dius.pact.core.support.json.JsonParser

/**
 * Builder for setting up consumer version selectors in provider JUnit tests.
 * See https://docs.pact.io/pact_broker/advanced_topics/consumer_version_selectors
 */
open class SelectorBuilder {
  val selectors: MutableList<ConsumerVersionSelectors> = mutableListOf()

  /**
   * The latest version from the main branch of each consumer, as specified by the consumer's mainBranch property.
   */
  fun mainBranch(): SelectorBuilder {
    selectors.add(ConsumerVersionSelectors.MainBranch)
    return this
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
  fun branch(name: String, consumer: String? = null, fallback: String? = null): SelectorBuilder {
    selectors.add(ConsumerVersionSelectors.Branch(name, consumer, fallback))
    return this
  }

  /**
   * All the currently deployed and currently released and supported versions of each consumer.
   */
  fun deployedOrReleased(): SelectorBuilder {
    selectors.add(ConsumerVersionSelectors.DeployedOrReleased)
    return this
  }

  /**
   * The latest version from any branch of the consumer that has the same name as the current branch of the provider.
   * Used for coordinated development between consumer and provider teams using matching feature branch names.
   */
  fun matchingBranch(): SelectorBuilder {
    selectors.add(ConsumerVersionSelectors.MatchingBranch)
    return this
  }

  /**
   * Any versions currently deployed to the specified environment
   */
  fun deployedTo(environment: String): SelectorBuilder {
    selectors.add(ConsumerVersionSelectors.DeployedTo(environment))
    return this
  }

  /**
   * Any versions currently released and supported in the specified environment
   */
  fun releasedTo(environment: String): SelectorBuilder {
    selectors.add(ConsumerVersionSelectors.ReleasedTo(environment))
    return this
  }

  /**
   * any versions currently deployed or released and supported in the specified environment
   */
  fun environment(environment: String): SelectorBuilder {
    selectors.add(ConsumerVersionSelectors.Environment(environment))
    return this
  }

  /**
   * All versions with the specified tag
   */
  fun tag(name: String): SelectorBuilder {
    selectors.add(ConsumerVersionSelectors.Tag(name))
    return this
  }

  /**
   * The latest version for each consumer with the specified tag
   */
  fun latestTag(name: String): SelectorBuilder {
    selectors.add(ConsumerVersionSelectors.LatestTag(name))
    return this
  }

  /**
   * Generic selector.
   *
   * * With just the tag name, returns all versions with the specified tag.
   * * With latest, returns the latest version for each consumer with the specified tag.
   * * With a fallback tag, returns the latest version for each consumer with the specified tag, falling back to the
   * fallbackTag if non is found with the specified tag.
   * * With a consumer name, returns the latest version for a specified consumer with the specified tag.
   * * With only latest, returns the latest version for each consumer. NOT RECOMMENDED as it suffers from race
   * conditions when pacts are published from multiple branches.
   */
  @Deprecated("Tags are deprecated in favor of branches", ReplaceWith("branch"))
  fun selector(tagName: String?, latest: Boolean?, fallbackTag: String?, consumer: String?): SelectorBuilder {
    selectors.add(ConsumerVersionSelectors.Selector(tagName, latest, consumer, fallbackTag))
    return this
  }

  /**
   * Selector in raw JSON form.
   */
  fun rawSelectorJson(json: String): SelectorBuilder {
    selectors.add(ConsumerVersionSelectors.RawSelector(JsonParser.parseString(json)))
    return this
  }

  /**
   * Construct the final list of consumer version selectors
   */
  fun build(): List<ConsumerVersionSelectors> = selectors
}
