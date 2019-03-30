package au.com.dius.pact.core.model

import com.google.gson.GsonBuilder
import mu.KLogging
import java.io.PrintWriter

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
}
