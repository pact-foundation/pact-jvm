package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelectors
import au.com.dius.pact.core.support.Auth.Companion.DEFAULT_AUTH_HEADER
import au.com.dius.pact.core.support.json.JsonParser
import org.apache.maven.plugin.MojoFailureException
import java.net.URL

data class EnablePending @JvmOverloads constructor(val providerTags: List<String> = emptyList())

abstract class BaseSelector {
  abstract fun toSelector(): ConsumerVersionSelectors
}

/**
 * The latest version from the main branch of each consumer, as specified by the consumer's mainBranch property.
 */
open class MainBranch() : BaseSelector() {
  override fun toSelector() = ConsumerVersionSelectors.MainBranch

  override fun toString(): String {
    return "MainBranch"
  }
}

/**
 * The latest version from a particular branch of each consumer, or for a particular consumer if the second
 * parameter is provided. If fallback is provided, falling back to the fallback branch if none is found from the
 * specified branch.
 */
open class Branch : BaseSelector() {
  var name: String = ""
  var consumer: String? = null
  var fallback: String? = null

  override fun toSelector(): ConsumerVersionSelectors {
    if (name.isEmpty()) {
      throw MojoFailureException("Branch selector requires the 'name' attribute")
    }

    return ConsumerVersionSelectors.Branch(name, consumer, fallback)
  }

  override fun toString(): String {
    return "Branch(name='$name', consumer=$consumer, fallback=$fallback)"
  }
}

/**
 * All the currently deployed and currently released and supported versions of each consumer.
 */
open class DeployedOrReleased(): BaseSelector() {
  override fun toSelector() = ConsumerVersionSelectors.DeployedOrReleased

  override fun toString(): String {
    return "DeployedOrReleased"
  }
}

/**
 * The latest version from any branch of the consumer that has the same name as the current branch of the provider.
 * Used for coordinated development between consumer and provider teams using matching feature branch names.
 */
open class MatchingBranch(): BaseSelector() {
  override fun toSelector() = ConsumerVersionSelectors.MatchingBranch

  override fun toString(): String {
    return "MatchingBranch"
  }
}

/**
 * Any versions currently deployed to the specified environment
 */
open class DeployedTo: BaseSelector() {
  var environment: String = ""

  override fun toSelector(): ConsumerVersionSelectors {
    if (environment.isEmpty()) {
      throw MojoFailureException("DeployedTo selector requires the 'environment' attribute")
    }

    return ConsumerVersionSelectors.DeployedTo(environment)
  }

  override fun toString(): String {
    return "DeployedTo(environment='$environment')"
  }
}

/**
 * Any versions currently released and supported in the specified environment
 */
open class ReleasedTo: BaseSelector() {
  var environment: String = ""

  override fun toSelector(): ConsumerVersionSelectors {
    if (environment.isEmpty()) {
      throw MojoFailureException("ReleasedTo selector requires the 'environment' attribute")
    }

    return ConsumerVersionSelectors.ReleasedTo(environment)
  }

  override fun toString(): String {
    return "ReleasedTo(environment='$environment')"
  }
}

/**
 * Any versions currently deployed or released and supported in the specified environment
 */
open class Environment: BaseSelector() {
  var name: String = ""

  override fun toSelector(): ConsumerVersionSelectors {
    if (name.isEmpty()) {
      throw MojoFailureException("Environment selector requires the 'name' attribute")
    }

    return ConsumerVersionSelectors.Environment(name)
  }

  override fun toString(): String {
    return "Environment(name='$name')"
  }
}

/**
 * All versions with the specified tag
 */
open class TagName: BaseSelector() {
  var name: String = ""

  override fun toSelector(): ConsumerVersionSelectors {
    if (name.isEmpty()) {
      throw MojoFailureException("TagName selector requires the 'name' attribute")
    }

    return ConsumerVersionSelectors.Tag(name)
  }

  override fun toString(): String {
    return "TagName(name='$name')"
  }
}

/**
 * The latest version for each consumer with the specified tag. If fallback is provided, will fall back to the
 * fallback tag if none is found with the specified tag
 */
open class LatestTag: BaseSelector() {
  var name: String = ""
  var fallback: String? = null

  override fun toSelector(): ConsumerVersionSelectors {
    if (name.isEmpty()) {
      throw MojoFailureException("LatestTag selector requires the 'name' attribute")
    }

    return ConsumerVersionSelectors.LatestTag(name, fallback)
  }

  override fun toString(): String {
    return "LatestTag(name='$name', fallback=$fallback)"
  }
}

/**
 * Corresponds to the old consumer version selectors
 */
open class Selector: BaseSelector() {
  var tag: String? = null
  var latest: Boolean? = null
  var consumer: String? = null
  var fallbackTag: String? = null

  override fun toSelector() = ConsumerVersionSelectors.Selector(tag, latest, consumer, fallbackTag)

  override fun toString(): String {
    return "Selector(tag=$tag, latest=$latest, consumer=$consumer, fallbackTag=$fallbackTag)"
  }
}

/**
 * Corresponds to the old consumer version selectors
 */
open class RawJson: BaseSelector() {
  var json: String? = null

  override fun toSelector() = ConsumerVersionSelectors.RawSelector(JsonParser.parseString(json.orEmpty()))

  override fun toString(): String {
    return "RawJson(json=$json)"
  }
}

/**
 * Bean to configure a pact broker to query
 */
data class PactBroker @JvmOverloads constructor(
  val url: URL? = null,
  @Deprecated("use consumer version selectors instead")
  val tags: List<String>? = emptyList(),
  val authentication: PactBrokerAuth? = null,
  val serverId: String? = null,
  var enablePending: EnablePending? = null,
  @Deprecated("use consumer version selectors instead")
  val fallbackTag: String? = null,
  val insecureTLS: Boolean? = false,
  val selectors: List<BaseSelector> = emptyList()
)

/**
 * Authentication for the pact broker, defaulting to Basic Authentication
 */
data class PactBrokerAuth @JvmOverloads constructor (
  val scheme: String? = "basic",
  val token: String? = null,
  val authHeaderName: String? = DEFAULT_AUTH_HEADER,
  val username: String? = null,
  val password: String? = null
)
