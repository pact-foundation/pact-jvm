package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import java.io.ByteArrayOutputStream
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart

abstract class BaseRequest : HttpPart() {

  /**
   * Sets up the request as a multipart file upload
   * @param partName The attribute name in the multipart upload that the file is included in
   * @param contentType The content type of the file data
   * @param contents File contents
   */
  fun withMultipartFileUpload(partName: String, filename: String, contentType: ContentType, contents: String) =
    withMultipartFileUpload(partName, filename, contentType.toString(), contents)

  /**
   * Sets up the request as a multipart file upload
   * @param partName The attribute name in the multipart upload that the file is included in
   * @param contentType The content type of the file data
   * @param contents File contents
   */
  fun withMultipartFileUpload(partName: String, filename: String, contentType: String, contents: String): BaseRequest {
    val multipart = MimeMultipart("form-data")
    val internetHeaders = InternetHeaders()
    internetHeaders.setHeader("Content-Disposition", "form-data; name=\"$partName\"; filename=\"$filename\"")
    internetHeaders.setHeader("Content-Type", contentType)
    multipart.addBodyPart(MimeBodyPart(internetHeaders, contents.toByteArray()))

    val stream = ByteArrayOutputStream()
    multipart.writeTo(stream)
    body = OptionalBody.body(stream.toByteArray(), ContentType(contentType))
    headers["Content-Type"] = listOf(multipart.contentType)

    return this
  }

  /**
   * If this request represents a multipart file upload
   */
  fun isMultipartFileUpload() = contentType().equals("multipart/form-data", ignoreCase = true)

  companion object {
    fun parseQueryParametersToMap(query: JsonValue?): Map<String, List<String>> {
      return when (query) {
        null -> emptyMap()
        is JsonValue.Object -> query.entries.entries.associate { entry ->
          val list = when (entry.value) {
            is JsonValue.Array -> entry.value.asArray().values.map { Json.toString(it) }
            else -> emptyList()
          }
          entry.key to list
        }
        is JsonValue.StringValue -> queryStringToMap(query.asString())
        else -> emptyMap()
      }
    }
  }
}
