package au.com.dius.pact.provider

import groovy.io.FileType
import groovy.json.JsonSlurper
import au.com.dius.pact.provider.org.fusesource.jansi.AnsiConsole

/**
 * Common provider utils
 */
class ProviderUtils {

  @SuppressWarnings('ParameterCount')
  static List loadPactFiles(def provider, File pactFileDir, def stateChange = null, boolean stateChangeUsesBody = true,
                            PactVerification verificationType = PactVerification.REQUST_RESPONSE,
                            List packagesToScan = [],
                            List pactFileAuthentication = []) {
    if (!pactFileDir.exists()) {
      throw new PactVerifierException("Pact file directory ($pactFileDir) does not exist")
    }

    if (!pactFileDir.isDirectory()) {
      throw new PactVerifierException("Pact file directory ($pactFileDir) is not a directory")
    }

    if (!pactFileDir.canRead()) {
      throw new PactVerifierException("Pact file directory ($pactFileDir) is not readable")
    }

    AnsiConsole.out().println("Loading pact files for provider ${provider.name} from $pactFileDir")

    List consumers = []
    pactFileDir.eachFileMatch FileType.FILES, ~/.*\.json/, {
      def pactJson = new JsonSlurper().parse(it)
      if (pactJson.provider.name == provider.name) {
        consumers << new ConsumerInfo(name: pactJson.consumer.name, pactFile: it, stateChange: stateChange,
          stateChangeUsesBody: stateChangeUsesBody, verificationType: verificationType, packagesToScan: packagesToScan,
          pactFileAuthentication: pactFileAuthentication)
      } else {
        AnsiConsole.out().println("Skipping ${it} as the provider names don't match provider.name: " +
          "${provider.name} vs pactJson.provider.name: ${pactJson.provider.name}")
      }
    }
    AnsiConsole.out().println("Found ${consumers.size()} pact files")
    consumers
  }

  static boolean pactFileExists(def pactFile) {
   pactFile && new File(pactFile as String).exists()
 }

  static PactVerification verificationType(ProviderInfo provider, ConsumerInfo consumer) {
   consumer.verificationType ?: provider.verificationType ?: PactVerification.REQUST_RESPONSE
  }

  static List packagesToScan(ProviderInfo providerInfo, ConsumerInfo consumer) {
   consumer.packagesToScan ?: providerInfo.packagesToScan
  }
}
