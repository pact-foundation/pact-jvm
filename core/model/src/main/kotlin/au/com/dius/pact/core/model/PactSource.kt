package au.com.dius.pact.core.model

import au.com.dius.pact.core.pactbroker.PactBrokerResult
import au.com.dius.pact.core.support.isNotEmpty
import java.io.File
import java.util.function.Supplier

/**
 * Represents the source of a Pact
 */
sealed class PactSource {
  open fun description() = toString()
}

/**
 * A source of a pact that comes from some URL
 */
sealed class UrlPactSource : PactSource() {
  abstract val url: String
  var encodePath: Boolean = true
}

data class DirectorySource @JvmOverloads constructor(
  val dir: File,
  val pacts: MutableMap<File, Pact> = mutableMapOf()
) : PactSource() {
  override fun description() = "Directory $dir"
}

data class PactBrokerSource<I> @JvmOverloads constructor(
  @Deprecated("Use url instead")
  val host: String?,
  @Deprecated("Use url instead")
  val port: String?,
  @Deprecated("Use url instead")
  val scheme: String? = "http",
  val pacts: MutableMap<Consumer, MutableList<Pact>> = mutableMapOf(),
  val url: String? = null
) : PactSource()
  where I : Interaction {
  override fun description(): String {
    return when {
      url.isNotEmpty() -> "Pact Broker $url"
      port == null -> "Pact Broker $scheme://$host"
      else -> "Pact Broker $scheme://$host:$port"
    }
  }
}

data class FileSource @JvmOverloads constructor(val file: File, val pact: Pact? = null) : PactSource() {
  override fun description() = "File $file"
}

data class UrlSource @JvmOverloads constructor(override val url: String, val pact: Pact? = null) : UrlPactSource() {
  override fun description() = "URL $url"
}

data class UrlsSource @JvmOverloads constructor(
  val url: List<String>,
  val pacts: MutableMap<String, Pact> = mutableMapOf()
) : PactSource() {
  fun addPact(url: String, pact: Pact) {
    pacts[url] = pact
  }
}

data class BrokerUrlSource @JvmOverloads constructor(
  override val url: String,
  val pactBrokerUrl: String,
  val attributes: Map<String, Any?> = mapOf(),
  val options: Map<String, Any> = mapOf(),
  val tag: String? = null,
  val result: PactBrokerResult? = null
) : UrlPactSource() {
  init {
    encodePath = false
  }
  override fun description() = if (tag == null) "Pact Broker $url" else "Pact Broker $url (Tag $tag)"

  companion object {
    fun fromResult(
      result: PactBrokerResult,
      options: Map<String, Any> = emptyMap(),
      tag: String? = null
    ): BrokerUrlSource {
      return BrokerUrlSource(
        result.source,
        result.pactBrokerUrl,
        emptyMap(),
        options,
        tag,
        result
      )
    }
  }
}

object InputStreamPactSource : PactSource()

object ReaderPactSource : PactSource()

object UnknownPactSource : PactSource()

@Suppress("ClassNaming")
data class S3PactSource(override val url: String) : UrlPactSource() {
  override fun description() = "S3 Bucket $url"
}

data class ClosurePactSource(val closure: Supplier<Any>) : PactSource()
