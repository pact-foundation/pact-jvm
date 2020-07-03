package au.com.dius.pact.provider

import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.core.model.Interaction
import org.apache.commons.io.FilenameUtils
import java.io.File

/**
 * Common provider utils
 */
@Suppress("ThrowsCount")
object ProviderUtils {

  @JvmStatic
  @JvmOverloads
  fun loadPactFiles(
    provider: IProviderInfo,
    pactFileDir: File,
    stateChange: Any? = null,
    stateChangeUsesBody: Boolean = true,
    verificationType: PactVerification = PactVerification.REQUEST_RESPONSE,
    packagesToScan: List<String> = emptyList(),
    pactFileAuthentication: List<String> = emptyList()
  ): List<IConsumerInfo> {
    if (!pactFileDir.exists()) {
      throw PactVerifierException("Pact file directory ($pactFileDir) does not exist")
    }

    if (!pactFileDir.isDirectory) {
      throw PactVerifierException("Pact file directory ($pactFileDir) is not a directory")
    }

    if (!pactFileDir.canRead()) {
      throw PactVerifierException("Pact file directory ($pactFileDir) is not readable")
    }

    println("Loading pact files for provider ${provider.name} from $pactFileDir")

    val consumers = mutableListOf<ConsumerInfo>()
    for (f in pactFileDir.listFiles { _, name -> FilenameUtils.isExtension(name, "json") }) {
      val pact = DefaultPactReader.loadPact(f)
      val providerName = pact.provider.name
      if (providerName == provider.name) {
        consumers.add(ConsumerInfo(pact.consumer.name,
          stateChange, stateChangeUsesBody, packagesToScan, verificationType,
          FileSource<Interaction>(f), pactFileAuthentication))
      } else {
        println("Skipping $f as the provider names don't match provider.name: " +
          "${provider.name} vs pactJson.provider.name: $providerName")
      }
    }
    println("Found ${consumers.size} pact files")
    return consumers
  }

  fun pactFileExists(pactFile: FileSource<Interaction>) = pactFile.file.exists()

  @JvmStatic
  fun verificationType(provider: IProviderInfo, consumer: IConsumerInfo): PactVerification {
    return consumer.verificationType ?: provider.verificationType ?: PactVerification.REQUEST_RESPONSE
  }

  @JvmStatic
  fun packagesToScan(providerInfo: IProviderInfo, consumer: IConsumerInfo): List<String> {
    return if (consumer.packagesToScan.isNotEmpty()) consumer.packagesToScan else providerInfo.packagesToScan
  }

  fun isS3Url(pactFile: Any?): Boolean {
    return pactFile is String && pactFile.toLowerCase().startsWith("s3://")
  }
}
