package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import java.util.Enumeration
import javax.mail.BodyPart
import javax.mail.Header
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

class MultipartMessageBodyMatcher : BodyMatcher {

  override fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    allowUnexpectedKeys: Boolean,
    matchingRules: MatchingRules
  ): List<BodyMismatch> {
    return when {
      expected.isMissing() -> emptyList()
      expected.isPresent() && actual.isNotPresent() -> listOf(BodyMismatch(expected.orEmpty(),
              null, "Expected a multipart body but was missing"))
      expected.isEmpty() && actual.isEmpty() -> emptyList()
      else -> {
        val expectedMultipart = parseMultipart(expected.valueAsString(), expected.contentType.contentType!!)
        val actualMultipart = parseMultipart(actual.valueAsString(), actual.contentType.contentType!!)
        compareHeaders(expectedMultipart, actualMultipart) + compareContents(expectedMultipart, actualMultipart)
      }
    }
  }

  private fun compareContents(expectedMultipart: BodyPart, actualMultipart: BodyPart): List<BodyMismatch> {
    val expectedContents = expectedMultipart.content.toString().trim()
    val actualContents = actualMultipart.content.toString().trim()
    return when {
      expectedContents.isEmpty() && actualContents.isEmpty() -> emptyList()
      expectedContents.isNotEmpty() && actualContents.isNotEmpty() -> emptyList()
      expectedContents.isEmpty() && actualContents.isNotEmpty() -> listOf(BodyMismatch(expectedContents,
              actualContents,
        "Expected no contents, but received ${actualContents.toByteArray().size} bytes of content"))
      else -> listOf(BodyMismatch(expectedContents,
              actualContents, "Expected content with the multipart, but received no bytes of content"))
    }
  }

  private fun compareHeaders(expectedMultipart: BodyPart, actualMultipart: BodyPart): List<BodyMismatch> {
    val mismatches = mutableListOf<BodyMismatch>()
    (expectedMultipart.allHeaders as Enumeration<Header>).asSequence().forEach {
      val header = actualMultipart.getHeader(it.name)
      if (header != null) {
        val actualValue = header.joinToString(separator = ", ")
        if (actualValue != it.value) {
          mismatches.add(BodyMismatch(it.toString(), null,
                  "Expected a multipart header '${it.name}' with value '${it.value}', but was '$actualValue'"))
        }
      } else {
        mismatches.add(BodyMismatch(it.toString(), null,
          "Expected a multipart header '${it.name}', but was missing"))
      }
    }

    return mismatches
  }

  private fun parseMultipart(body: String, contentType: String): BodyPart {
    val multipart = MimeMultipart(ByteArrayDataSource(body, contentType))
    return multipart.getBodyPart(0)
  }
}
