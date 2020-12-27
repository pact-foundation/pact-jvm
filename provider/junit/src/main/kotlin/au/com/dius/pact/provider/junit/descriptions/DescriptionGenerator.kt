package au.com.dius.pact.provider.junit.descriptions

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.support.isNotEmpty
import org.junit.runner.Description
import org.junit.runners.model.TestClass

/**
 * Class responsible for building junit tests Description.
 */
class DescriptionGenerator<I : Interaction>(
  private val testClass: TestClass,
  @Deprecated("Pass the pact source and consumer name in")
  private val pact: Pact<I>?,
  private val pactSource: PactSource? = null,
  private val consumerName: String? = null
) {

  /**
   * Builds an instance of junit Description adhering with this logic for building the name:
   * If the PactSource is of type <code>BrokerUrlSource</code> and its tag is not empty then
   * the test name will be "#consumername [tag:#tagname] - Upon #interaction".
   * For all the other cases "#consumername - Upon #interaction"
   * @param interaction the Interaction under test
   */
  fun generate(interaction: Interaction): Description {
    val messagePrefix = if (interaction is Message) {
      "Generates message '${interaction.description}' ${pending()}"
    } else {
      "Upon ${interaction.description}${pending()}"
    }
    return Description.createTestDescription(testClass.javaClass,
      "${consumerName()} ${getTagDescription()}- $messagePrefix ")
  }

  private fun consumerName(): String {
    val source = pactSource ?: pact?.source
    val name = when {
      source is BrokerUrlSource -> source.result?.name ?: pact?.consumer?.name
      consumerName.isNotEmpty() -> consumerName
      else -> pact?.consumer?.name
    }
    return name ?: "Unknown consumer"
  }

  private fun pending(): String {
    val source = pactSource ?: pact?.source
    return if (source is BrokerUrlSource) {
      if (source.result != null && source.result!!.pending) {
        " <PENDING>"
      } else {
        ""
      }
    } else {
      ""
    }
  }

  private fun getTagDescription(): String {
    val source = pactSource ?: pact?.source
    if (source is BrokerUrlSource) {
      val tag = source.tag
      return if (tag.isNotEmpty()) "[tag:${source.tag}] " else ""
    }
    return ""
  }
}
