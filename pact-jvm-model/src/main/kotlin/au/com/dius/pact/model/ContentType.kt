package au.com.dius.pact.model

private val jsonRegex = Regex("application\\/.*json")
private val xmlRegex = Regex("application\\/.*xml")

data class ContentType(val contentType: String) {

  fun isJson(): Boolean = jsonRegex.matches(contentType.toLowerCase())

  fun isXml(): Boolean = xmlRegex.matches(contentType.toLowerCase())
}
