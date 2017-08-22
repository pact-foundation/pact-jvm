package au.com.dius.pact.model

private val jsonRegex = Regex("application\\/.*json")
private val xmlRegex = Regex("application\\/.*xml")

data class ContentType(val contentType: String) {

  fun isJson(): Boolean {
    return jsonRegex.matches(contentType.toLowerCase())
  }

  fun isXml(): Boolean {
    return xmlRegex.matches(contentType.toLowerCase())
  }

}
