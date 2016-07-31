package au.com.dius.pact.model

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j

/**
 * Class to write out a pact to a file
 */
@Slf4j
class PactWriter {

  /**
   * Writes out the pact to the provided pact file
   * @param pact Pact to write
   * @param writer Writer to write out with
   * @param pactSpecVersion Pact version to use to control writing
   */
  static writePact(Pact pact, PrintWriter writer, PactSpecVersion pactSpecVersion = PactSpecVersion.V3) {
    pact.sortInteractions()
    Map jsonData = pact.toMap(pactSpecVersion)
    writer.print(JsonOutput.prettyPrint(JsonOutput.toJson(jsonData)))
  }

}
