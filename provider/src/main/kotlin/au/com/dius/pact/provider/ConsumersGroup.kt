package au.com.dius.pact.provider

import java.io.File
import java.net.URL

/**
 * Consumers grouped by pacts in a directory or an S3 bucket
 */
data class ConsumersGroup @JvmOverloads constructor (
  var name: String,
  var pactFileLocation: File? = null,
  var stateChange: Any? = null,
  var stateChangeUsesBody: Boolean = false,
  var stateChangeTeardown: Boolean = false,
  var include: Regex = Regex(".*\\.json$")
) {

  fun url(path: String) {
    stateChange = URL(path)
  }
}
