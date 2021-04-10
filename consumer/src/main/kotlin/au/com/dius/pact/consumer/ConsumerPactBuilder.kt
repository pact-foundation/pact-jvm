package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSpecVersion
import org.w3c.dom.Document
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

open class ConsumerPactBuilder(
  /**
   * Returns the name of the consumer
   * @return consumer name
   */
  val consumerName: String
  ) {
  private var version: PactSpecVersion = PactSpecVersion.V3
  val interactions: MutableList<Interaction> = mutableListOf()

  /**
   * Name the provider that the consumer has a pact with
   * @param provider provider name
   */
  fun hasPactWith(provider: String): PactDslWithProvider {
    return PactDslWithProvider(this, provider, version)
  }

  fun pactSpecVersion(version: PactSpecVersion): ConsumerPactBuilder {
    this.version = version
    return this
  }

  companion object {
    /**
     * Name the consumer of the pact
     * @param consumer Consumer name
     */
    @JvmStatic
    fun consumer(consumer: String): ConsumerPactBuilder {
      return ConsumerPactBuilder(consumer)
    }

    fun jsonBody(): PactDslJsonBody {
      return PactDslJsonBody()
    }

    @Throws(TransformerException::class)
    fun xmlToString(body: Document): String {
      val transformer = TransformerFactory.newInstance().newTransformer()
      transformer.setOutputProperty(OutputKeys.INDENT, "yes")
      val result = StreamResult(StringWriter())
      val source = DOMSource(body)
      transformer.transform(source, result)
      return result.writer.toString()
    }
  }
}
