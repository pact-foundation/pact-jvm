package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonException
import au.com.dius.pact.core.support.json.JsonParser
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import mu.KLogging
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.io.RandomAccessFile
import java.io.StringWriter
import java.nio.charset.Charset

enum class PactWriteMode {
  MERGE, OVERWRITE
}

/**
 * Class to write out a pact to a file
 */
interface PactWriter {
  /**
   * Writes out the pact to the provided pact file
   * @param pact Pact to write
   * @param writer Writer to write out with
   * @param pactSpecVersion Pact version to use to control writing
   */
  fun writePact(pact: Pact, writer: PrintWriter, pactSpecVersion: PactSpecVersion) : Result<Int, Throwable>

  /**
   * Writes out the pact to the provided pact file
   * @param pact Pact to write
   * @param writer Writer to write out with
   */
  fun writePact(pact: Pact, writer: PrintWriter) : Result<Int, Throwable>

  /**
   * Writes out the pact to the provided pact file in a manor that is safe for parallel execution
   * @param pactFile File to write to
   * @param pact Pact to write
   * @param pactSpecVersion Pact version to use to control writing
   */
  fun writePact(pactFile: File, pact: Pact, pactSpecVersion: PactSpecVersion) : Result<Int, Throwable>
}

/**
 * Default implementation of a Pact writer
 */
object DefaultPactWriter : PactWriter, KLogging() {

  /**
   * Writes out the pact to the provided pact file
   * @param pact Pact to write
   * @param writer Writer to write out with
   * @param pactSpecVersion Pact version to use to control writing
   */
  override fun writePact(pact: Pact, writer: PrintWriter, pactSpecVersion: PactSpecVersion) : Result<Int, Throwable> {
    pact.sortInteractions()
    val json = Json.prettyPrint(pact.toMap(pactSpecVersion))
    writer.println(json)
    return Ok(json.toByteArray().size)
  }

  /**
   * Writes out the pact to the provided pact file in V3 format
   * @param pact Pact to write
   * @param writer Writer to write out with
   */
  override fun writePact(pact: Pact, writer: PrintWriter) : Result<Int, Throwable> {
    return writePact(pact, writer, PactSpecVersion.V3)
  }

  /**
   * Writes out the pact to the provided pact file in a manor that is safe for parallel execution
   * @param pactFile File to write to
   * @param pact Pact to write
   * @param pactSpecVersion Pact version to use to control writing
   */
  @Synchronized
  override fun writePact(pactFile: File, pact: Pact, pactSpecVersion: PactSpecVersion) : Result<Int, Throwable> {
    return if (pactWriteMode() == PactWriteMode.MERGE && pactFile.exists() && pactFile.length() > 0) {
      val raf = RandomAccessFile(pactFile, "rw")
      val lock = raf.channel.lock()
      try {
        val source = FileSource(pactFile)
        val json = JsonParser.parseString(readFileUtf8(raf)).asObject()
        val existingPact = DefaultPactReader.pactFromJson(json, source)
        val result = PactMerge.merge(existingPact, pact)
        if (!result.ok) {
          throw InvalidPactException(result.message)
        }
        raf.seek(0)
        val swriter = StringWriter()
        val writer = PrintWriter(swriter)
        writePact(pact, writer, pactSpecVersion)
        val bytes = swriter.toString().toByteArray()
        raf.setLength(bytes.size.toLong())
        raf.write(bytes)
        Ok(bytes.size)
      } finally {
        lock.release()
        raf.close()
      }
    } else {
      pactFile.parentFile.mkdirs()
      pactFile.printWriter().use { writePact(pact, it, pactSpecVersion) }
    }
  }

  private fun pactWriteMode(): PactWriteMode {
    return when (System.getProperty("pact.writer.overwrite")) {
      "true" -> PactWriteMode.OVERWRITE
      else -> PactWriteMode.MERGE
    }
  }

  private fun readFileUtf8(file: RandomAccessFile): String {
    val buffer = ByteArray(128)
    val data = ByteArrayOutputStream()

    file.seek(0)
    var count = file.read(buffer)
    while (count > 0) {
      data.write(buffer, 0, count)
      count = file.read(buffer)
    }

    return String(data.toByteArray(), Charset.forName("UTF-8"))
  }
}
