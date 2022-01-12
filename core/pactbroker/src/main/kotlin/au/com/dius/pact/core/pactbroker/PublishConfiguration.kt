package au.com.dius.pact.core.pactbroker

/**
 * Model to encapsulate the options used when publishing a Pact file
 */
data class PublishConfiguration @JvmOverloads constructor(
  /**
   * Version of the consumer that is publishing the Pact file
   */
  val consumerVersion: String,
  /**
   * Tags to use to tag the Pact file with
   */
  val tags: List<String> = emptyList(),
  /**
   * Source control branch name of the consumer
   */
  val branchName: String? = null,
  /**
   * Consumer branch URL
   */
  val consumerBuildUrl: String? = null
)
