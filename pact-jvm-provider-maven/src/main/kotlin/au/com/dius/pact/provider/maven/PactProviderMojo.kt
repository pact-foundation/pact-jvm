package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.PactVerifierException
import au.com.dius.pact.provider.ProviderUtils
import au.com.dius.pact.provider.ProviderVerifier
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.settings.Settings
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest
import org.apache.maven.settings.crypto.SettingsDecrypter
import org.fusesource.jansi.AnsiConsole
import java.io.File
import java.util.function.Function
import java.util.function.Supplier

/**
 * Pact Verify Maven Plugin
 */
@Mojo(name = "verify", requiresDependencyResolution = ResolutionScope.TEST)
open class PactProviderMojo : AbstractMojo() {

  @Parameter(defaultValue = "\${project.testClasspathElements}", required = true)
  private lateinit var classpathElements: List<String>

  @Parameter
  var systemPropertyVariables: Map<String, String> = mutableMapOf()

  @Parameter
  lateinit var serviceProviders: List<Provider>

  @Parameter
  private var configuration = mutableMapOf<String, String>()

  @Parameter(required = true, defaultValue = "\${project.version}")
  private lateinit var projectVersion: String

  @Parameter(defaultValue = "\${settings}", readonly = true)
  private lateinit var settings: Settings

  @Component
  private lateinit var decrypter: SettingsDecrypter

  @Parameter(defaultValue = "true")
  var failIfNoPactsFound: Boolean = true

  override fun execute() {
    AnsiConsole.systemInstall()

    systemPropertyVariables.forEach { (property, value) ->
      System.setProperty(property, value)
    }

    val failures = mutableMapOf<Any, Any>()
    val verifier = providerVerifier().let {
      it.projectHasProperty = Function { p: String -> this.propertyDefined(p) }
      it.projectGetProperty = Function { p: String -> this.property(p) }
      it.pactLoadFailureMessage = Function { consumer: ConsumerInfo ->
        "You must specify the pact file to execute for consumer '${consumer.name}' (use <pactFile> or <pactUrl>)"
      }
      it.checkBuildSpecificTask = Function { false }
      it.providerVersion = Supplier { projectVersion }

      it.projectClasspath = Supplier {
        val urls = classpathElements.map { File(it).toURI().toURL() }
        urls.toTypedArray()
      }
      it
    }

    serviceProviders.forEach { provider ->
      val consumers = mutableListOf<ConsumerInfo>()
      consumers.addAll(provider.consumers)
      if (provider.pactFileDirectory != null) {
          consumers.addAll(loadPactFiles(provider, provider.pactFileDirectory))
      }
      if (provider.pactFileDirectories.isNotEmpty()) {
        provider.pactFileDirectories.forEach {
          consumers.addAll(loadPactFiles(provider, it))
        }
      }
      if (provider.pactBrokerUrl != null || provider.pactBroker != null) {
        loadPactsFromPactBroker(provider, consumers)
      }

      if (consumers.isEmpty() && failIfNoPactsFound) {
        throw MojoFailureException("No pact files were found for provider '${provider.name}'")
      }

      provider.consumers = consumers

      failures.putAll(verifier.verifyProvider(provider) as Map<out Any, Any>)
    }

    if (failures.isNotEmpty()) {
      verifier.displayFailures(failures)
      AnsiConsole.systemUninstall()
      throw MojoFailureException("There were ${failures.size} pact failures")
    }
  }

  open fun providerVerifier() = ProviderVerifier()

  fun loadPactsFromPactBroker(provider: Provider, consumers: MutableList<ConsumerInfo>) {
    val pactBroker = provider.pactBroker
    val pactBrokerUrl = pactBroker?.url ?: provider.pactBrokerUrl
    val options = mutableMapOf<String, Any>()

    if (pactBroker?.authentication != null) {
      options["authentication"] = listOf("basic", provider.pactBroker.authentication.username,
        provider.pactBroker.authentication.password)
    } else if (!pactBroker?.serverId.isNullOrEmpty()) {
      val serverDetails = settings.getServer(provider.pactBroker!!.serverId)
      val request = DefaultSettingsDecryptionRequest(serverDetails)
      val result = decrypter.decrypt(request)
      options["authentication"] = listOf("basic", serverDetails.username, result.server.password)
    }

    if (pactBroker != null && pactBroker.tags != null && pactBroker.tags.isNotEmpty()) {
      pactBroker.tags.forEach { tag ->
        consumers.addAll(provider.hasPactsFromPactBrokerWithTag(options, pactBrokerUrl.toString(), tag))
      }
    } else {
      consumers.addAll(provider.hasPactsFromPactBroker(options, pactBrokerUrl.toString()))
    }
  }

  open fun loadPactFiles(provider: Any, pactFileDir: File): List<ConsumerInfo> {
    return try {
      ProviderUtils.loadPactFiles(provider, pactFileDir)
    } catch (e: PactVerifierException) {
      log.warn("Failed to load pact files from directory $pactFileDir", e)
      emptyList()
    }
  }

  private fun propertyDefined(key: String) = System.getProperty(key) != null || configuration.containsKey(key)

  private fun property(key: String) = System.getProperty(key, configuration.get(key))
}
