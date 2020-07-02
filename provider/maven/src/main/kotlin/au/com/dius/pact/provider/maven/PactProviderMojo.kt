package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.core.support.toUrl
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.PactVerifierException
import au.com.dius.pact.provider.ProviderUtils
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.ProviderVersion
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.reporters.ReporterManager
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest
import java.io.File
import java.util.function.Function
import java.util.function.Supplier

/**
 * Pact Verify Maven Plugin
 */
@Mojo(name = "verify", requiresDependencyResolution = ResolutionScope.TEST)
open class PactProviderMojo : PactBaseMojo() {

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

  @Parameter(defaultValue = "true")
  var failIfNoPactsFound: Boolean = true

  @Parameter(defaultValue = "\${project.build.directory}", readonly = true)
  lateinit var buildDir: File

  @Parameter(defaultValue = "console")
  lateinit var reports: List<String>

  override fun execute() {
    systemPropertyVariables.forEach { (property, value) ->
      System.setProperty(property, value)
    }

    val verifier = providerVerifier().let { verifier ->
      verifier.projectHasProperty = Function { p: String -> this.propertyDefined(p) }
      verifier.projectGetProperty = Function { p: String -> this.property(p) }
      verifier.pactLoadFailureMessage = Function { consumer: ConsumerInfo ->
        "You must specify the pact file to execute for consumer '${consumer.name}' (use <pactFile> or <pactUrl>)"
      }
      verifier.checkBuildSpecificTask = Function { false }
      verifier.providerVersion = ProviderVersion { projectVersion }

      verifier.projectClasspath = Supplier { classpathElements.map { File(it).toURI().toURL() } }

      if (reports.isNotEmpty()) {
        val reportsDir = File(buildDir, "reports/pact")
        verifier.reporters = reports.map { name ->
          if (ReporterManager.reporterDefined(name)) {
            val reporter = ReporterManager.createReporter(name, reportsDir, verifier)
            reporter
          } else {
            throw MojoFailureException("There is no defined reporter named '$name'. Available reporters are: " +
              "${ReporterManager.availableReporters()}")
          }
        }
      }

      verifier
    }

    try {
      val failures = serviceProviders.flatMap { provider ->
        val consumers = mutableListOf<IConsumerInfo>()
        consumers.addAll(provider.consumers)
        if (provider.pactFileDirectory != null) {
          consumers.addAll(loadPactFiles(provider, provider.pactFileDirectory!!))
        }
        if (provider.pactFileDirectories != null && provider.pactFileDirectories!!.isNotEmpty()) {
          provider.pactFileDirectories!!.forEach {
            consumers.addAll(loadPactFiles(provider, it))
          }
        }
        if (provider.pactBrokerUrl != null || provider.pactBroker != null) {
          loadPactsFromPactBroker(provider, consumers)
        }
        if (provider.pactFileDirectory == null &&
          (provider.pactFileDirectories == null || provider.pactFileDirectories!!.isEmpty()) &&
          provider.pactBrokerUrl == null && provider.pactBroker == null && (
            pactBrokerUrl != null || pactBrokerServerId != null)) {
          loadPactsFromPactBroker(provider, consumers, brokerClientOptions())
        }

        if (consumers.isEmpty() && failIfNoPactsFound) {
          throw MojoFailureException("No pact files were found for provider '${provider.name}'")
        }

        provider.consumers = consumers

        verifier.verifyProvider(provider)
      }.filterIsInstance<VerificationResult.Failed>()

      if (failures.isNotEmpty()) {
        verifier.displayFailures(failures)
        val nonPending = failures.filterNot { it.pending }
        if (nonPending.isNotEmpty()) {
          throw MojoFailureException("There were ${nonPending.sumBy { it.failures.size }} non-pending pact failures")
        }
      }
    } finally {
      verifier.finaliseReports()
    }
  }

  open fun providerVerifier(): IProviderVerifier = ProviderVerifier()

  fun loadPactsFromPactBroker(
    provider: Provider,
    consumers: MutableList<IConsumerInfo>,
    brokerClientOptions: MutableMap<String, Any> = mutableMapOf()
  ) {
    val pactBroker = provider.pactBroker
    val pactBrokerUrl = pactBroker?.url ?: provider.pactBrokerUrl ?: pactBrokerUrl.toUrl()
    val options = brokerClientOptions.toMutableMap()

    if (pactBroker?.authentication != null) {
      if ("bearer" == provider.pactBroker?.authentication?.scheme || provider.pactBroker?.authentication?.token != null) {
        options["authentication"] = listOf("bearer", provider.pactBroker!!.authentication!!.token)
      } else if ("basic" == provider.pactBroker?.authentication?.scheme) {
        options["authentication"] = listOf(provider.pactBroker!!.authentication!!.scheme, provider.pactBroker!!.authentication!!.username,
                provider.pactBroker!!.authentication!!.password)
      }
    } else if (!pactBroker?.serverId.isNullOrEmpty()) {
      val serverDetails = settings.getServer(provider.pactBroker!!.serverId)
      val request = DefaultSettingsDecryptionRequest(serverDetails)
      val result = decrypter.decrypt(request)
      options["authentication"] = listOf("basic", serverDetails.username, result.server.password)
    }

    when {
      pactBroker?.enablePending != null -> {
        if (pactBroker.enablePending!!.providerTags.isEmpty()) {
          throw MojoFailureException("""
            |No providerTags: To use the pending pacts feature, you need to provide the list of provider names for the provider application version that will be published with the verification results.
            |
            |For instance, if you tag your provider with 'master':
            |
            |<enablePending>
            |    <providerTags>
            |        <tag>master</tag>
            |    </providerTags>
            |</enablePending>
          """.trimMargin())
        }
        val selectors = pactBroker.tags?.map { ConsumerVersionSelector(it, true) } ?: emptyList()
        consumers.addAll(provider.hasPactsFromPactBrokerWithSelectors(options +
          mapOf("enablePending" to true, "providerTags" to pactBroker.enablePending!!.providerTags),
          pactBrokerUrl.toString(), selectors))
      }
      pactBroker?.tags != null && pactBroker.tags.isNotEmpty() -> {
        val selectors = pactBroker.tags.map { ConsumerVersionSelector(it, true) }
        consumers.addAll(provider.hasPactsFromPactBrokerWithSelectors(options, pactBrokerUrl.toString(), selectors))
      }
      else -> consumers.addAll(provider.hasPactsFromPactBrokerWithSelectors(options, pactBrokerUrl.toString(), emptyList()))
    }
  }

  open fun loadPactFiles(provider: IProviderInfo, pactFileDir: File): List<IConsumerInfo> {
    return try {
      ProviderUtils.loadPactFiles(provider, pactFileDir)
    } catch (e: PactVerifierException) {
      log.warn("Failed to load pact files from directory $pactFileDir", e)
      emptyList()
    }
  }

  private fun propertyDefined(key: String) = System.getProperty(key) != null || configuration.containsKey(key)

  private fun property(key: String) = System.getProperty(key, configuration[key])
}
