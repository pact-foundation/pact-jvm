package au.com.dius.pact.provider.maven

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.settings.Settings
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest
import org.apache.maven.settings.crypto.SettingsDecrypter

abstract class PactBaseMojo : AbstractMojo() {
  @Parameter
  protected var pactBrokerUrl: String? = null

  @Parameter
  protected var pactBrokerServerId: String? = null

  @Parameter
  protected var pactBrokerToken: String? = null

  @Parameter
  protected var pactBrokerUsername: String? = null

  @Parameter
  protected var pactBrokerPassword: String? = null

  @Parameter(defaultValue = "basic")
  protected var pactBrokerAuthenticationScheme: String? = null

  @Parameter(defaultValue = "\${settings}", readonly = true)
  protected lateinit var settings: Settings

  @Component
  protected lateinit var decrypter: SettingsDecrypter

  protected fun brokerClientOptions(): MutableMap<String, Any> {
    val options = mutableMapOf<String, Any>()
    if (!pactBrokerToken.isNullOrEmpty()) {
      pactBrokerAuthenticationScheme = "bearer"
      options["authentication"] = listOf(pactBrokerAuthenticationScheme, pactBrokerToken)
    } else if (!pactBrokerUsername.isNullOrEmpty()) {
      options["authentication"] = listOf(pactBrokerAuthenticationScheme ?: "basic", pactBrokerUsername,
        pactBrokerPassword)
    } else if (!pactBrokerServerId.isNullOrEmpty()) {
      val serverDetails = settings.getServer(pactBrokerServerId)
      val request = DefaultSettingsDecryptionRequest(serverDetails)
      val result = decrypter.decrypt(request)
      options["authentication"] = listOf(pactBrokerAuthenticationScheme ?: "basic", serverDetails.username,
        result.server.password)
    }
    return options
  }
}
