package au.com.dius.pact.model

import com.google.gson.GsonBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Class to write out a pact to a file
 */
@Slf4j
@CompileStatic
class PactWriter {

  /**
   * Writes out the pact to the provided pact file
   * @param pact Pact to write
   * @param writer Writer to write out with
   * @param pactSpecVersion Pact version to use to control writing
   */
  static void writePact(Pact pact, PrintWriter writer, PactSpecVersion pactSpecVersion = PactSpecVersion.V3) {
    pact.sortInteractions()
    Map jsonData = pact.toMap(pactSpecVersion)
    def gson = new GsonBuilder().setPrettyPrinting().create()
    gson.toJson(jsonData, writer)
  }

}
