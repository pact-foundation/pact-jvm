package au.com.dius.pact.provider.junitsupport

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.support.isNotEmpty

class TestDescription(
  val interaction: Interaction,
  val pactSource: PactSource?,
  val consumerName: String?,
  val consumer: Consumer?
) {
  fun generateDescription(): String {
    val messagePrefix = if (interaction.isAsynchronousMessage()) {
      "Generates message '${interaction.description}' ${pending()}"
    } else {
      "Upon ${interaction.description}${pending()}"
    }
    return "${consumerName()} ${getTagDescription()}- $messagePrefix "
  }

  private fun consumerName(): String {
    val name = when {
      pactSource is BrokerUrlSource -> pactSource.result?.name ?: consumer?.name
      consumerName.isNotEmpty() -> consumerName
      else -> consumer?.name
    }
    return name ?: "Unknown consumer"
  }

  private fun pending(): String {
    return if (pactSource is BrokerUrlSource) {
      if (pactSource.result != null && pactSource.result!!.pending) {
        " <PENDING>"
      } else {
        ""
      }
    } else {
      ""
    }
  }

  private fun getTagDescription(): String {
    if (pactSource is BrokerUrlSource) {
      val tag = pactSource.tag
      return if (tag.isNotEmpty()) "[tag:${pactSource.tag}] " else ""
    }
    return ""
  }
}
