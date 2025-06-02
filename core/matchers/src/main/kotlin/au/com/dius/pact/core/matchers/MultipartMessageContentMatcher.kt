package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.IHttpPart
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.isNotEmpty
import io.github.oshai.kotlinlogging.KLogging
import io.pact.plugins.jvm.core.InteractionContents
import java.util.Enumeration
import javax.mail.BodyPart
import javax.mail.Header
import javax.mail.internet.ContentDisposition
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimePart
import javax.mail.util.ByteArrayDataSource

class MultipartMessageContentMatcher : ContentMatcher {

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
        val expectedMultipart = MimeMultipart(ByteArrayDataSource(expected.orEmpty(), expected.contentType.toString()))
        val actualMultipart = MimeMultipart(ByteArrayDataSource(actual.orEmpty(), actual.contentType.toString()))
        BodyMatchResult(null, compareParts(expectedMultipart, actualMultipart, context))
      }
    }
  }

  private fun compareParts(
    expectedMultipart: MimeMultipart,
    actualMultipart: MimeMultipart,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    val matchResults = mutableListOf<BodyItemMatchResult>()

    logger.debug { "Comparing multiparts: expected has ${expectedMultipart.count} part(s), " +
      "actual has ${actualMultipart.count} part(s)" }

    if (expectedMultipart.count != actualMultipart.count) {
      matchResults.add(BodyItemMatchResult("$", listOf(BodyMismatch(expectedMultipart.count, actualMultipart.count,
        "Expected a multipart message with ${expectedMultipart.count} part(s), " +
          "but received one with ${actualMultipart.count} part(s)"))))
    }

    for (i in 0 until expectedMultipart.count) {
      val expectedPart = expectedMultipart.getBodyPart(i)
      if (i < actualMultipart.count) {
        val actualPart = actualMultipart.getBodyPart(i)
        var path = i.toString()
        if (expectedPart is MimePart) {
          val disposition = expectedPart.getHeader("Content-Disposition", null)
          if (disposition != null) {
            val cd = ContentDisposition(disposition)
            val parameter = cd.getParameter("name")
            if (parameter.isNotEmpty()) {
              path = parameter
            }
          }
        }

        val headerResult = compareHeaders(path, expectedPart, actualPart)
        logger.debug { "Comparing part $i: header mismatches ${headerResult.size}" }
        val bodyMismatches = compareContents(path, expectedPart, actualPart, context)
        logger.debug { "Comparing part $i: content mismatches ${bodyMismatches.size}" }
        matchResults.add(BodyItemMatchResult(path, headerResult + bodyMismatches))
      }
    }

    return matchResults
  }

  override fun setupBodyFromConfig(
    bodyConfig: Map<String, Any?>
  ): Result<List<InteractionContents>, String> {
    return Result.Ok(listOf(InteractionContents("",
      OptionalBody.body(
        bodyConfig["body"].toString().toByteArray(),
        ContentType("multipart/form-data")
      )
    )))
  }

  @Suppress("UnusedPrivateMember")
  private fun compareContents(
    path: String,
    expectedMultipart: BodyPart,
    actualMultipart: BodyPart,
    context: MatchingContext
  ): List<BodyMismatch> {
    val expected = bodyPartTpHttpPart(expectedMultipart)
    val actual = bodyPartTpHttpPart(actualMultipart)
    logger.debug {
      "Comparing multipart contents: ${expected.determineContentType()} -> ${actual.determineContentType()}"
    }
    val result = Matching.matchBody(expected, actual, context.extractPath("\$.$path"))
    return result.bodyResults.flatMap { matchResult ->
      matchResult.result.map {
        it.copy(path = path + it.path.removePrefix("$"))
      }
    }
  }

  private fun bodyPartTpHttpPart(multipart: BodyPart): IHttpPart {
    return HttpRequest(headers = mutableMapOf("content-type" to listOf(multipart.contentType)),
      body = OptionalBody.body(multipart.inputStream.readAllBytes(), ContentType(multipart.contentType)))
  }

  private fun compareHeaders(
    path: String,
    expectedMultipart: BodyPart,
    actualMultipart: BodyPart
  ): List<BodyMismatch> {
    val mismatches = mutableListOf<BodyMismatch>()
    (expectedMultipart.allHeaders as Enumeration<Header>).asSequence().forEach {
      val header = actualMultipart.getHeader(it.name)
      if (header != null) {
        val actualValue = header.joinToString(separator = ", ")
        if (actualValue != it.value) {
          mismatches.add(BodyMismatch(it.toString(), null,
            "Expected a multipart header '${it.name}' with value '${it.value}', but was '$actualValue'",
            path + "." + it.name))
        }
      } else {
        if (it.name.equals("Content-Type", ignoreCase = true)) {
          logger.debug { "Ignoring missing Content-Type header" }
        } else {
          mismatches.add(BodyMismatch(it.toString(), null,
            "Expected a multipart header '${it.name}', but was missing", path + "." + it.name))
        }
      }
    }

    return mismatches
  }

  companion object : KLogging()
}
