package au.com.dius.pact.model

import java.io.File

/**
 * Represents the source of a Pact
 */
interface PactSource

data class DirectorySource @JvmOverloads constructor(val dir: File,
                                                     val pacts: MutableMap<File, Pact> = mutableMapOf()) : PactSource

data class PactBrokerSource @JvmOverloads constructor(val host: String,
                                                      val port: String,
                                                      val pacts: MutableMap<Consumer, List<Pact>> = mutableMapOf()) : PactSource

data class FileSource(val file: File, val pact: Pact) : PactSource
data class UrlSource(val url: String, val pact: Pact) : PactSource

data class UrlsSource @JvmOverloads constructor(val url: List<String>,
                                                val pacts: MutableMap<String, Pact> = mutableMapOf()) : PactSource
