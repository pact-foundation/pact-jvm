package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import mu.KLogging
import java.util.Enumeration
import javax.mail.BodyPart
import javax.mail.Header
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

class MultipartMessageBodyMatcher : BodyMatcher {

  override fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    context: MatchingContext
  ): BodyMatchResult {
    return when {
      expected.isMissing() -> BodyMatchResult(null, emptyList())
      expected.isPresent() && actual.isNotPresent() -> BodyMatchResult(null,
        listOf(BodyItemMatchResult("$", listOf(BodyMismatch(expected.orEmpty(),
          null, "Expected a multipart body but was missing")))))
      expected.isEmpty() && actual.isEmpty() -> BodyMatchResult(null, emptyList())
      else -> {
        val expectedMultipart = parseMultipart(expected.valueAsString(), expected.contentType.toString())
        val actualMultipart = parseMultipart(actual.valueAsString(), actual.contentType.toString())
        BodyMatchResult(null, compareHeaders(expectedMultipart, actualMultipart, context) +
          compareContents(expectedMultipart, actualMultipart, context))
      }
    }
  }

  private fun compareContents(
    expectedMultipart: BodyPart,
    actualMultipart: BodyPart,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    val expectedContents = expectedMultipart.content.toString().trim()
    val actualContents = actualMultipart.content.toString().trim()
    return when {
      expectedContents.isEmpty() && actualContents.isEmpty() -> emptyList()
      expectedContents.isNotEmpty() && actualContents.isNotEmpty() -> emptyList()
      expectedContents.isEmpty() && actualContents.isNotEmpty() -> listOf(BodyItemMatchResult("$",
        listOf(BodyMismatch(expectedContents, actualContents,
        "Expected no contents, but received ${actualContents.toByteArray().size} bytes of content"))))
      else -> listOf(BodyItemMatchResult("$", listOf(BodyMismatch(expectedContents,
        actualContents, "Expected content with the multipart, but received no bytes of content"))))
    }
  }

  private fun compareHeaders(
    expectedMultipart: BodyPart,
    actualMultipart: BodyPart,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    val mismatches = mutableListOf<BodyItemMatchResult>()
    (expectedMultipart.allHeaders as Enumeration<Header>).asSequence().forEach {
      val header = actualMultipart.getHeader(it.name)
      if (header != null) {
        val actualValue = header.joinToString(separator = ", ")
        if (actualValue != it.value) {
          mismatches.add(BodyItemMatchResult(it.name, listOf(BodyMismatch(it.toString(), null,
                  "Expected a multipart header '${it.name}' with value '${it.value}', but was '$actualValue'"))))
        }
      } else {
        if (it.name.equals("Content-Type", ignoreCase = true)) {
          logger.debug { "Ignoring missing Content-Type header" }
        } else {
          mismatches.add(BodyItemMatchResult(it.name, listOf(BodyMismatch(it.toString(), null,
            "Expected a multipart header '${it.name}', but was missing"))))
        }
      }
    }

    return mismatches
  }

  private fun parseMultipart(body: String, contentType: String): BodyPart {
    val multipart = MimeMultipart(ByteArrayDataSource(body, contentType))
    return multipart.getBodyPart(0)
  }

  companion object : KLogging()
}
