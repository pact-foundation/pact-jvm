package au.com.dius.pact.model

import java.io.File

/**
 * Represents the source of a Pact
 */
interface PactSource

/**
 * A source of a pact that comes from some URL
 */
interface UrlPactSource : PactSource {
  val url: String
}

data class DirectorySource @JvmOverloads constructor(val dir: File,
                                                     val pacts: MutableMap<File, Pact> = mutableMapOf()) : PactSource

data class PactBrokerSource @JvmOverloads constructor(val host: String,
                                                      val port: String,
                                                      val pacts: MutableMap<Consumer, List<Pact>> = mutableMapOf()) : PactSource

data class FileSource @JvmOverloads constructor(val file: File, val pact: Pact? = null) : PactSource

data class UrlSource @JvmOverloads constructor(override val url: String, val pact: Pact? = null) : UrlPactSource

data class UrlsSource @JvmOverloads constructor(val url: List<String>,
                                                val pacts: MutableMap<String, Pact> = mutableMapOf()) : PactSource

data class BrokerUrlSource @JvmOverloads constructor(override val url: String, val attributes: Map<String, String> = mapOf()) : UrlPactSource

object InputStreamPactSource : PactSource

object ReaderPactSource : PactSource

object UnknownPactSource : PactSource

data class S3PactSource(override val url: String) : UrlPactSource
