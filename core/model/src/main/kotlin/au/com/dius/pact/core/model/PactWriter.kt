package au.com.dius.pact.core.model

import com.google.gson.GsonBuilder
import mu.KLogging
import java.io.File
import java.io.PrintWriter
import java.io.RandomAccessFile
import java.io.StringWriter

enum class PactWriteMode {
  MERGE, OVERWRITE
}

/**
 * Class to write out a pact to a file
 */
object PactWriter : KLogging() {

  /**
   * Writes out the pact to the provided pact file
   * @param pact Pact to write
   * @param writer Writer to write out with
   * @param pactSpecVersion Pact version to use to control writing
   */
  @JvmStatic
  @JvmOverloads
  fun <I> writePact(pact: Pact<I>, writer: PrintWriter, pactSpecVersion: PactSpecVersion = PactSpecVersion.V3)
    where I : Interaction {
    pact.sortInteractions()
    val jsonData = pact.toMap(pactSpecVersion)
    val gson = GsonBuilder().setPrettyPrinting().create()
    gson.toJson(jsonData, writer)
  }

  /**
   * Writes out the pact to the provided pact file in a manor that is safe for parallel execution
   * @param pactFile File to write to
   * @param pact Pact to write
   * @param pactSpecVersion Pact version to use to control writing
   */
  @JvmStatic
  @Synchronized
  fun <I> writePact(pactFile: File, pact: Pact<I>, pactSpecVersion: PactSpecVersion)
    where I : Interaction {
    if (pactWriteMode() == PactWriteMode.MERGE && pactFile.exists() && pactFile.length() > 0) {
      val raf = RandomAccessFile(pactFile, "rw")
      val lock = raf.channel.lock()
      try {
        val pactReaderClass = Class.forName("au.com.dius.pact.core.model.PactReader")
        val loadPact = pactReaderClass.getDeclaredMethod("loadPact", Class.forName("java.lang.Object"))
        val existingPact = loadPact.invoke(null, readLines(raf)) as Pact<I>
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

  private fun readLines(file: RandomAccessFile): String {
    val data = StringBuilder()
    var line = file.readLine()
    while (line != null) {
      data.append(line)
      line = file.readLine()
    }
    return data.toString()
  }
}
