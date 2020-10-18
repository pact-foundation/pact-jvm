package au.com.dius.pact.provider.junitsupport.loader

/**
 * Implementation of [PactLoader] that downloads pacts from given urls containing versions to be filtered in
 * from system properties.
 *
 * @see VersionedPactUrl usage instructions
 */
open class VersionedPactUrlLoader(urls: Array<String>) : PactUrlLoader(expandVariables(urls)) {
  constructor(pactUrl: VersionedPactUrl) : this(pactUrl.urls) {}

  companion object {
    @JvmStatic
    fun expandVariables(urls: Array<String>): Array<String> {
      return urls.asList().map { urlWithVariables -> expandVariables(urlWithVariables) }.toTypedArray()
    }

    private fun expandVariables(urlWithVariables: String): String {
      var urlWithVersions = urlWithVariables
      require(variablesToExpandFound(urlWithVersions)) {
        "$urlWithVersions contains no variables to expand in the format \${...}. " +
          "Consider using @PactUrl or providing expandable variables."
      }
      for ((key, value) in System.getProperties()) {
        urlWithVersions = urlWithVersions.replace(String.format("\${%s}", key), value.toString())
      }
      require(!variablesToExpandFound(urlWithVersions)) {
        "$urlWithVersions contains variables that could not be any of the system properties. " +
          "Define a system property to replace them or remove the variables from the URL."
      }
      return urlWithVersions
    }

    private fun variablesToExpandFound(urlWithVersions: String): Boolean {
      return urlWithVersions.matches(Regex(".*\\$\\{[a-z\\.]+\\}.*"))
    }
  }
}
